(ns nagaino.cache
  (:use [somnium.congomongo.config :only [*mongo-config*]] )
  (:require [somnium.congomongo :as mongo]
            [clojure.tools.logging :as log] ))

;;; config

(def mongo-url (System/getenv "MONGOHQ_URL"))

;;; connection management

(defn split-mongo-url [url]
  (let [matcher (re-matcher #"^.*://(?:(.*?):(.*?)@)?(.*?):(\d+)/(.*)$" url)]
    (when (.find matcher)
      (zipmap [:match :user :pass :host :port :db] (re-groups matcher)) )))

(defn maybe-init []
  (dosync
   (when-not (mongo/connection? *mongo-config*)
     (let [config (split-mongo-url mongo-url)]
       (mongo/set-connection!
        (mongo/make-connection (:db config)
         :host (:host config) :port (Integer. (:port config))
         (mongo/mongo-options :socket-timeout 1000) ))
       (when-let [user (:user config)]
         (mongo/authenticate user (:pass config)) )))))

;;; fetch

(defn find-first [fn seq]
  (first (filter fn seq)) )

(defn update-from-cache [n-urls results]
  (map (fn [n-url]
	 (let [u (-> n-url :long_url_path first)]
	   (if-let [r (find-first #(= u (:short_url %)) results)]
	     (assoc n-url
	       :done? true
	       :long_url_path (concat (:long_url_path r)
				      (rest (:long_url_path n-url)) )
	       :long_url (:long_url r) :cached (:short_url r) )
	     n-url )))
       n-urls) )

(defn expand-from-cache [sq]
  (if mongo-url
    (try
      (maybe-init)
      (->> sq (map #(->> % :long_url_path first))
           (#(mongo/fetch :nagainocache :where {:short_url {:$in %}} ))
           (update-from-cache sq) )
      (catch Exception e
        (log/error (str "caught exception: " (.getMessage e)))
        sq ))
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

(defn gather-caching-urls [sq]
  (reduce (fn [r v]
	    (if (or (:error v) (= (:cached v) (:short_url v))
		    (-> v :long_url_path rest empty?) )
	      r
	      (into r (not-cached-list v)) ))
	  () sq ))

(def cache-update-agent (agent nil))

(defn do-update-cache [_ sq]
  (try
    (maybe-init)
    (let [r (gather-caching-urls sq)]
      (or (empty? r) (mongo/mass-insert! :nagainocache r)) )
    (catch Exception e
      (log/error (str "caught exception: " (.getMessage e)))
      nil )))

(defn update-cache [sq]
  (when mongo-url
    (send-off cache-update-agent do-update-cache sq) )
  sq )
