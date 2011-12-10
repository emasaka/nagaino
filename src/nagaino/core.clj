(ns nagaino.core
  (:use [compojure.core]
	[ring.adapter.jetty]
	[ring.util.codec :only [url-decode]]
	[clojure.contrib.json :only [json-str]]
	[clojure.contrib.str-utils :only [re-split]]
	[nagaino.expandurls :only [expand-urls]]
	[nagaino.view :only [format-html]] )
  (:require [compojure.handler :as handler]
	    [compojure.route :as route] ))

(defn transform-result [fmt urls]
  (case fmt
	"json_hash" (zipmap (map #(:short_url %) urls) urls)
	"json_simple" (zipmap (map #(:short_url %) urls)
			      (map #(:long_url %) urls) )
	urls ))

(defn query->longurl [params]
  (let [urls (params "q")]
    (if urls
      (expand-urls urls) )))

(defn text->longurl [params]
  (let [urls (:shortUrls params)]
    (if urls
      (->> urls url-decode (re-split #"\n") expand-urls)
      () )))

(defn res-json [seq]
  {:headers {"Content-Type" "application/json; charset=utf-8"}
   :body (json-str {"status_code" 200 "data" {"expand" seq}}) } )

(defn res-html [seq]
  {:headers {"Content-Type" "text/html; charset=utf-8"}
   :body (format-html seq) } )

(defn res [fmt seq]
  ((if (= fmt "html") res-html res-json) seq) )

(defn api-expand [params]
  (->> params
       query->longurl
       (transform-result (params "format"))
       (res (params "format")) ))

(defn api-expand-text [params]
  (->> params
       text->longurl
       (transform-result (:format params))
       (res (:format params)) ))

(defroutes route
  (GET "/api/v0/expand" [:as params]
       (api-expand (:query-params params)) )
  (POST "/api/v0/expandText" {params :params}
	(api-expand-text params) )
  (route/files "/" {:root "./resources/public"}) )

(def app (handler/api route))

(defn -main [& _]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "8080"))]
    (run-jetty app {:port port})))
