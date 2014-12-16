(ns nagaino.expandurls
  (:use [clojure.string :only [join]]
	[clojure.java.io :only [resource]]
	[ring.util.codec :only [url-encode]]
	[nagaino.cache :only [expand-from-cache update-cache]] )
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.tools.logging :as log] )
  (:import [java.io InputStreamReader PushbackReader]
           [me.geso.regexp_trie RegexpTrie] ))

;;; config

(defn read-from-resource-file [file]
  (with-open [strm (-> file resource .openStream
                       InputStreamReader. PushbackReader. )]
    (read strm) ))

(def config (read-from-resource-file "config.clj"))

(defn seq->prefix-search-regex [sq]
  (re-pattern
   (str "\\A"
        ;; quoting `.' in regexp by string/replace is workaround
        (clojure.string/replace
         (.regexp (reduce (fn [r v] (.add r v) r) (RegexpTrie.) sq))
         "." "\\." ))))

(def shorturl-regex
  (seq->prefix-search-regex
   (-> (:shorturl-hosts config)
       (into (:bitly-hosts config))
       (into (:htnto-hosts config) ))))

(def bitlyurl-regex
     (seq->prefix-search-regex (:bitly-hosts config) ))

(def bitly-user (System/getenv "BITLY_USER"))

(def bitly-key (System/getenv "BITLY_KEY"))

(def BITLY-MAX-URLS 15)

(def htnto-regex
     (seq->prefix-search-regex (:htnto-hosts config) ))

(def HTNTO-MAX-URLS 15)

;;; structure

(defstruct NagainoUrl :short_url :long_url_path :long_url :done? :error :cached)

(defn string->nagaino-url [^String url]
  (struct NagainoUrl url (list url) nil false nil nil) )

(defn nagaino-url->map [n-url]
  (let [m (dissoc (into {} n-url) :done? :cached)]
    (if (:error m) m (dissoc m :error)) ))

(defstruct Expm :short_url :long_url :error)

;;; HTTP access

(def REQUEST-OPTIONS {:throw-exceptions false
                      :conn-timeout 3000
                      :socket-timeout 3000 })

(def LOCATION-REQUEST-OPTIONS (assoc REQUEST-OPTIONS
                                     :follow-redirects false ))

(defn parse-location-res [res]
  (let [code (:status res)]
    (if (<= 301 code 307) [((res :headers) "location") nil] [nil code]) ))

(defn url-location [^String url]
  (-> url
      (client/head LOCATION-REQUEST-OPTIONS)
      parse-location-res ))

(defn url->expm [^String url]
  (try
    (let [[u msg] (url-location url)] (struct Expm url u msg))
    (catch java.net.SocketTimeoutException e
      (log/warn (str "Timed out: " url))
      (struct Expm url nil "Timed out") )))

(defn keywordize [m]
  (reduce (fn [r v]
            (let [k (v 0)] (conj r {(if (string? k) (keyword k) k) (v 1)})) )
          {} m ))

;;; expand bit.ly URLs

(defn bitly-query-url [sq]
  (str "http://api.bitly.com/v3/expand?format=json&login=" bitly-user
       "&apiKey=" bitly-key "&"
       (join "&" (map #(str "shortUrl=" (url-encode %)) sq)) ))

(defn parse-bitly-res [res sq]
  (if (= (:status res) 200)
    (let [dat (-> res :body json/parse-string)]
      (if (= (dat "status_code") 200)
        (map keywordize ((dat "data") "expand"))
        (map #(struct Expm % nil (dat "status_txt")) sq) ))
    (map #(struct Expm % nil (:status res)) sq) ))

(defn bitly-urls->expms [sq]
  (try
    (-> sq bitly-query-url
        (client/get REQUEST-OPTIONS)
        (parse-bitly-res sq) )
    (catch java.net.SocketTimeoutException e
      (log/warn "Timed out: bit.ly")
      (map #(struct Expm % nil "Timed out") sq) )))

(defn bitly-urls->expm-seq [sq]
  (doall (map #(future (bitly-urls->expms %))
	      (partition-all BITLY-MAX-URLS sq) )))

;;; expand htn.to URLs

(defn htnto-query-url [sq]
  (str "http://b.hatena.ne.jp/api/htnto/expand?"
       (join "&" (map #(str "shortUrl=" %) sq)) ))

(defn parse-htnto-res [res sq]
  (if (= (:status res) 200)
    (let [dat (-> res :body json/parse-string)]
      (if (= (dat "status_code") "200")
        (map keywordize ((dat "data") "expand"))
        (map #(struct Expm % nil (dat "status_txt")) sq) ))
    (map #(struct Expm % nil (:status res)) sq) ))

(defn htnto-urls->expms [sq]
  (try
    (-> sq htnto-query-url
        (client/get REQUEST-OPTIONS)
        (parse-htnto-res sq) )
    (catch java.net.SocketTimeoutException e
      (log/warn "Timed out: htn.to")
      (map #(struct Expm % nil "Timed out") sq) )))

(defn htnto-urls->expm-seq [sq]
  (doall (map #(future (htnto-urls->expms %))
	      (partition-all HTNTO-MAX-URLS sq) )))

;;; update table

(defn urls->expm-seq [sq]
  (doall (map #(future (url->expm %)) sq)) )

(defn url-types [url]
  (condp #(re-find %1 %2) url
    bitlyurl-regex :bitlyurls
    htnto-regex :htntourls
    :urls ))

(defn expand-by-type [url-groups]
  (map deref (concat (urls->expm-seq (:urls url-groups))
                     (bitly-urls->expm-seq (:bitlyurls url-groups))
                     (htnto-urls->expm-seq (:htntourls url-groups))) ))

(defn update-table [table sq]
  (->> sq
       (filter #(-> % :done? not))
       (map #(-> % :long_url_path first))
       distinct
       (group-by url-types)
       expand-by-type
       flatten
       (reduce (fn [r v] (conj r {(:short_url v) v})) table) ))

;;; update n-urls from table

(defn update-done [n-url]
  (if (re-find shorturl-regex (-> n-url :long_url_path first))
    n-url
    (assoc n-url :done? true :long_url (-> n-url :long_url_path first)) ))

(defn update-looped [n-url]
  (let [long-urls (:long_url_path n-url)
	last-urls (rest long-urls) ]
    (if (some #(= % (first long-urls)) last-urls)
      (assoc n-url :long_url_path last-urls :done? true :error "looped")
      n-url )))

(defn update-status [n-url]
  (if (:done? n-url) n-url (-> n-url update-looped update-done)) )

(defn add-url [n-url url]
  (assoc n-url :long_url_path (cons url (:long_url_path n-url))) )

(defn do-expand-from-table [table n-url]
  (let [r (table (-> n-url :long_url_path first))]
    (cond (nil? r) n-url
	  (:error r) (assoc n-url :done? true :error (:error r))
	  :else (recur table (add-url n-url (:long_url r))) )))

(defn expand-from-table [table n-url]
  (if (:done? n-url) n-url (do-expand-from-table table n-url)) )

;;; main part

(defn expand-nagaino-urls-1 [table n-urls]
  (let [n-urls-2 (expand-from-cache n-urls)
	table2 (update-table table n-urls-2) ]
    [table2
     (map #(->> % (expand-from-table table2) update-status) n-urls-2) ] ))

(defn expand-nagaino-urls [table n-urls]
  (let [[table2 r] (expand-nagaino-urls-1 table n-urls)]
    (if (every? :done? r) r (recur table2 r)) ))

(defn expand-urls [sq]
  (->> sq
       distinct
       (map #(-> % string->nagaino-url update-done))
       (expand-nagaino-urls {})
       update-cache
       (map nagaino-url->map) ))
