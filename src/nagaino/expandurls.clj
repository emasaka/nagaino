(ns nagaino.expandurls
  (:use [clojure-http.client :only [request *follow-redirects*]]
	[clojure.contrib.str-utils :only [str-join]]
	[clojure.contrib.io :only [with-in-reader]]
	[clojure.java.io :only [resource]]
	[clojure.contrib.json :only [read-json]]
	[ring.util.codec :only [url-encode]]
	[nagaino.cache :only [expand-from-cache update-cache]] ))

;;; config

(def config (with-in-reader (resource "config.clj") (read)) )

(defn seq->prefix-search-regex [sq]
  (re-pattern
   (str "\\A(?:"
	(str-join "|" (map #(java.util.regex.Pattern/quote %) sq))
	")" )))

(def shorturl-regex
     (seq->prefix-search-regex
      (into (:shorturl-hosts config) (:bitly-hosts config)) ))

(def bitlyurl-regex
     (seq->prefix-search-regex (:bitly-hosts config) ))

(def bitly-user (System/getenv "BITLY_USER"))

(def bitly-key (System/getenv "BITLY_KEY"))

;;; structure

(defstruct NagainoUrl :short_url :long_url_path :long_url :done? :error :cached)

(defn string->nagaino-url [#^String url]
  (struct NagainoUrl url (list url) nil false nil nil) )

(defn nagaino-url->map [n-url]
  (let [m (dissoc (into {} n-url) :done? :cached)]
    (if (:error m) m (dissoc m :error)) ))

;;; HTTP access

(defn url-location [#^String url]
  (let [res (binding [*follow-redirects* false] (request url "HEAD"))
	code (:code res) ]
    (cond (>= code 400) [nil code (:msg res)]
	  (>= code 300) [(-> res :headers :location first) code nil]
	  :else [nil code (:msg res)] )))

(defn map-array->map [ary key-key val-key]
  (reduce (fn [r v] (assoc r (v key-key) (v val-key))) {} ary) )

(defn expand-bitly-urls [sq]
  (let [url (str
	     "http://api.bitly.com/v3/expand?format=json&login=" bitly-user
	     "&apiKey=" bitly-key "&"
	     (str-join "&" (map #(str "shortUrl=" (url-encode %)) sq)) )
	res (request url) ]
    (if (= (:code res) 200)
      [(-> res :body-seq first read-json :data :expand
	   (#(map-array->map % :short_url :long_url)) ) nil]
      [nil (:msg res)] )))

;;; main part

(defn do-update-done [n-url]
  (assoc n-url :done? :true :long_url (-> n-url :long_url_path first)) )

(defn update-done [n-url]
  (if (re-find shorturl-regex (-> n-url :long_url_path first))
    n-url
    (do-update-done n-url) ))

(defn update-looped [n-url]
  (let [long-urls (:long_url_path n-url)
	last-urls (rest long-urls) ]
    (if (some #(= % (first long-urls)) last-urls)
      (assoc n-url :long_url_path last-urls :done? true :error "looped")
      n-url )))

(defn update-status [n-url]
  (if (:done? n-url) n-url (-> n-url update-looped update-done)) )

(defn assoc-cons [mp key val]
  (assoc mp key (cons val (mp key))) )

(defn expand-n-url-1 [n-url]
  (future (let [[u code msg] (-> n-url :long_url_path first url-location)]
	    (if u
	      (assoc-cons n-url :long_url_path u)
	      (let [r (do-update-done n-url)]
		(if (>= code 400) r (assoc r :error msg)) )))))

(defn expand-bitly-n-urls-1 [sq]
  (let [[m err] (expand-bitly-urls (map #(-> % :long_url_path first) sq))]
    (map (if err
	   #(assoc (do-update-done %) :error err)
	   (fn [n-url]
	     (if-let [r (m (-> n-url :long_url_path first))]
	       (assoc-cons n-url :long_url_path r)
	       (assoc (do-update-done n-url) :error "Not Found") )))
	 sq )))

(defn expand-bitly-n-urls [sq]
  (if (empty? sq)
    sq
    (->> sq (partition-all 15) (map #(future (expand-bitly-n-urls-1 %)))) ))

(defn expand-nagaino-urls-1 [n-urls]
  (->> n-urls
       expand-from-cache
       (reduce (fn [r v]
		 (cond (:done? v) (assoc-cons r :dones v)
		       (re-find bitlyurl-regex (-> v :long_url_path first))
		         (assoc-cons r :bitlyurls v)
		       :else (assoc-cons r :n-urls v) ))
	       {:dones () :n-urls () :bitlyurls ()} )
       (#(list (:dones %)
	       (map deref (into (map expand-n-url-1 (:n-urls %))
				(expand-bitly-n-urls (:bitlyurls %)) ))))
       flatten
       (map update-status) ))

(defn expand-nagaino-urls [n-urls]
  (let [r (expand-nagaino-urls-1 n-urls)]
    (if (every? :done? r) r (recur r)) ))

(defn expand-urls [sq]
  (->> sq
       distinct
       (map #(-> % string->nagaino-url update-done))
       expand-nagaino-urls
       update-cache
       (map nagaino-url->map) ))
