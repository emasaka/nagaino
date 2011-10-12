(ns nagaino.core
  (:use [compojure.core]
	[ring.adapter.jetty]
	[ring.util.codec :only [url-decode]]
	[clojure.contrib.json :only [json-str]]
	[clojure.contrib.str-utils :only [re-split]]
	[nagaino.expandurls :only [expand-urls]] )
  (:require [compojure.handler :as handler]
	    [compojure.route :as route] ))

(defn transform-result [fmt urls]
  (case fmt
	"json_hash" (zipmap (map #(:short_url %) urls) urls)
	"json_simple" (zipmap (map #(:short_url %) urls)
			      (map #(:long_url %) urls) )
	urls ))

(defn query->longurl [params]
  (let [qp (:query-params params)]
    (->> (qp "q") expand-urls (transform-result (qp "format"))) ))

(defn text->longurl [params]
  (->> params :shortUrls url-decode (re-split #"\n") expand-urls
       (transform-result (:format params)) ))

(defn res-json [seq]
  {:headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json-str {"status_code" 200 "data" {"expand" seq}}) } )

(defroutes route
  (GET "/api/v0/expand" [:as params]
       (-> params query->longurl res-json) )
  (POST "/api/v0/expandText" {params :params}
	(-> params text->longurl res-json) )
  (route/resources "/") )

(def app (handler/api route))

(defn -main [& _]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (run-jetty app {:port port})))
