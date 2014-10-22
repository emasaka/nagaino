(ns leiningen.buildfile
  (:require [clojure.string])
  (:require [clojure.java.io :as io]) )

(defn make-url-re [urls]
  (str
   "^(?:"
   (clojure.string/join
    "|"
    ;; java.util.regex.Pattern#quote don't go along well with `/'
    (map #(clojure.string/replace % #"[/.]" "\\\\$0") urls) )
   ")" ))

(defn read-url-conf []
  (let [conf (read-string (slurp "resources/config.clj"))]
    (into (:shorturl-hosts conf) (:bitly-hosts conf)) ))

(defn apply-template [tmpl dat]
  ;; workaround
  (clojure.string/replace tmpl "{{URL_RE}}" (:URL_RE dat)) )

(defn build-js []
  (.mkdirs (io/file "resources/public/js"))
  (spit "resources/public/js/nagainolet.js"
        (apply-template (slurp "resources/templates/js/nagainolet.js")
                        {:URL_RE (make-url-re (read-url-conf))} )))

(defn buildfile [project]
  (build-js) )
