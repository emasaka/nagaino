(ns nagaino.core
  (:use [compojure.core]
	[ring.adapter.jetty]
	[ring.util.codec :only [url-decode]]
        [ring.middleware.file-info :only [wrap-file-info]]
	[clojure.string :only [split]]
	[nagaino.expandurls :only [expand-urls]]
	[nagaino.view :only [format-html]] )
  (:require [ring.middleware.defaults :refer :all]
	    [compojure.route :as route]
            [cheshire.core :as json] ))

(defn transform-result [fmt urls]
  (case fmt
	"json_hash" (zipmap (map #(:short_url %) urls) urls)
	"json_simple" (zipmap (map #(:short_url %) urls)
			      (map #(:long_url %) urls) )
	urls ))

(defn query->longurl [params]
  (if-let [urls (params "q")]
    (expand-urls urls)
    () ))

(defn text->longurl [params]
  (if-let [urls (:shortUrls params)]
    (-> urls url-decode (split #"\r?\n") expand-urls)
    () ))

(defn res-json [seq]
  {:headers {"Content-Type" "application/json; charset=utf-8"
             "Access-Control-Allow-Origin" "*" }
   :body (json/generate-string {"status_code" 200 "data" {"expand" seq}}) } )

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

(def app (wrap-file-info (wrap-defaults route api-defaults)))
