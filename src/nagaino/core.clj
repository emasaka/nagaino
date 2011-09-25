(ns nagaino.core
  (:use [compojure.core]
	[ring.adapter.jetty]
	[ring.util.codec :only [url-decode]]
	[clojure.contrib.json :only [json-str]]
	[clojure.contrib.str-utils :only [re-split]]
	[nagaino.expandurls :only [expand-urls]] )
  (:require [compojure.handler :as handler]
	    [compojure.route :as route] ))

(defn query->longurl [params]
  (-> params :query-params (get "shortUrl") expand-urls) )

(defn json-res [seq]
  {:headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json-str {"status_code" 200 "data" {"expand" seq}}) } )

(defroutes route
  (GET "/api/v0/expand" [:as params]
       (-> params query->longurl json-res) )
  (POST "/api/v0/expandText" {params :params}
	(->> params :shortUrls url-decode (re-split #"\n") expand-urls
	     json-res ))
  (route/resources "/") )

(def app (handler/api route))

(defn -main [& _]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (run-jetty app {:port port})))
