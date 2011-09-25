(ns nagaino.cache
  (:use [somnium.congomongo]
	[somnium.congomongo.config :only [*mongo-config*]]
	[clojure.contrib.seq-utils :only [find-first]] ))

;;; config

(def mongo-url (System/getenv "MONGOHQ_URL"))

;;; copied from
;;; http://thecomputersarewinning.com/post/clojure-heroku-noir-mongo
;;; and modified

(defn split-mongo-url [url]
  (let [matcher (re-matcher #"^.*://(.*?):(.*?)@(.*?):(\d+)/(.*)$" url)]
    (if (.find matcher)
      (zipmap [:match :user :pass :host :port :db] (re-groups matcher))
      (let [matcher (re-matcher #"^.*://(.*?):(\d+)/(.*)$" url)]
	(when (.find matcher)
	  (zipmap [:match :host :port :db] (re-groups matcher)) )))))

(defn maybe-init []
  (when (not (connection? *mongo-config*))
    (let [config (split-mongo-url mongo-url)]
      (mongo! :db (:db config) :host (:host config)
	      :port (Integer. (:port config)) )
      (if-let [user (:user config)]
	(authenticate user (:pass config)) ))))

;;; my codes

;;; fetch

(defn update-from-cache [n-urls results]
  (map (fn [n-url]
	 (let [u (-> n-url :long_url_path first)]
	   (if-let [r (find-first #(= u (:short_url %)) results)]
	     (assoc n-url
	       :done? true
	       :long_url_path (concat (:long_url_path r)
				      (rest (:long_url_path n-url)) )
	       :long_url (:long_url r) :cached (:sort_url r) )
	     n-url )))
       n-urls) )

(defn expand-from-cache [sq]
  (if mongo-url
    (do (maybe-init)
	(let [rs (->> sq (map #(->> % :long_url_path first))
		      (#(fetch :nagainocache :where {:short_url {:$in %}} ))) ]
	  (update-from-cache sq rs) ))
    sq ))

;;; insert

(defn lists-starts-before [lst item r]
  (if (= (first lst) item)
    r
    (recur (rest lst) item (cons lst r)) ))

(defn not-cached-list [n-url]
  (map #(array-map :long_url_path (reverse %)
		   :short_url (first %) :long_url (:long_url n-url ))
       (let [rev (reverse (:long_url_path n-url))]
	 (lists-starts-before rev (or (:cached n-url) (last rev)) ()) )))

(defn update-cache [sq]
  (when mongo-url
    (maybe-init)
    (let [r (reduce (fn [r v]
		      (if (or (:error v) (= (:cached v) (:short_url v)))
			r
			(into r (not-cached-list v)) )) () sq )]
      (or (empty? r) (mass-insert! :nagainocache r)) ))
  sq )
