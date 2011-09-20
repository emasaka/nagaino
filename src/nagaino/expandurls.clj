(ns nagaino.expandurls
  (:use [clojure-http.client :only [request *follow-redirects*]]
	[clojure.contrib.str-utils :only [str-join]]
	[clojure.contrib.io :only [with-in-reader]]
	[clojure.java.io :only [resource]]
	[clojure.contrib.json :only [read-json]]
	[ring.util.codec :only [url-encode]] ))

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

(defstruct NagainoUrl :short_url :long_url_path :done? :error :long_url)

(defn string->NagainoUrl [#^String url]
  (struct NagainoUrl url (list url) false nil nil) )

;;; HTTP access

(defn url-location [#^String url]
  (let [res (binding [*follow-redirects* false] (request url "HEAD"))
	loc (-> res :headers :location) ]
    (and loc (first loc)) ))

(defn expand-bitly-urls [sq]
  (let [url (str
	     "http://api.bitly.com/v3/expand?format=json&login=" bitly-user
	     "&apiKey=" bitly-key "&"
	     (str-join "&" (map #(str "shortUrl=" (url-encode %)) sq)) )
	res (request url) ]
    (if (= (:code res) 200)
      (reduce (fn [r v] (assoc r (:short_url v) (:long_url v))) {}
	      (-> (read-json (first (:body-seq res))) :data :expand) )
      {} )))

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
  (future (if-let [newurl (-> n-url :long_url_path first url-location)]
	    (assoc-cons n-url :long_url_path newurl)
	    (do-update-done n-url) )))

(defn expand-bitly-n-urls [sq]
  (if (empty? sq)
    sq
    (let [m (expand-bitly-urls (map #(-> % :long_url_path first) sq))]
      (map (fn [n-url]
	     (if-let [r (m (-> n-url :long_url_path first))]
	       (assoc-cons n-url :long_url_path r)
	       (assoc n-url :done? true :error "not found") ))
	   sq ))))

(defn deref-if-future [x]
  (if (future? x) @x x) )

(defn expand-nagaino-urls-1 [n-urls]
  (->> n-urls
       (reduce (fn [r v]
		 (cond (:done? v) (assoc-cons r :dones v)
		       (re-find bitlyurl-regex (-> v :long_url_path first))
		         (assoc-cons r :bitlyurls v)
		       :else (assoc-cons r :n-urls v) ))
	       {:dones () :n-urls () :bitlyurls ()} )
       (#(into (into (:dones %) (map expand-n-url-1 (:n-urls %)))
	       (expand-bitly-n-urls (:bitlyurls %)) ))
       (map #(-> % deref-if-future update-status)) ))

(defn expand-nagaino-urls [n-urls]
  (let [r (expand-nagaino-urls-1 n-urls)]
    (if (every? :done? r) r (recur r)) ))

(defn expand-urls [sq]
  (->> sq
       distinct
       (map #(-> % string->NagainoUrl update-done))
       expand-nagaino-urls
       (map #(dissoc (into {} %) :done?)) ))
