(ns nagaino.core
  (:use [compojure.core]
	[ring.adapter.jetty]
	[clojure.contrib.json :only [json-str]]
	[nagaino.expandurls :only [expand-urls]] )
  (:require [compojure.handler :as handler] ))

(defn query->longurl [params]
  (-> params :query-params (get "shortUrl") expand-urls) )

(defn json-res [seq]
  (json-str {"status_code" 200 "data" {"expand" seq}}) )

(defroutes route
  (GET "/api/v0/expand" [:as params]
       {:headers {"Content-Type" "application/json; charset=utf-8"}
	:body (-> params query->longurl json-res) }))

(def app (handler/api route))

(defn -main [& _]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (run-jetty app {:port port})))
