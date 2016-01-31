(ns leiningen.buildfile
  (:refer-clojure :exclude [replace])
  (:require [cemerick.pomegranate :refer [add-dependencies]]
            [clojure.string :refer [replace]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [cheshire.core :as json] ))

(add-dependencies
 :coordinates '[[me.geso/regexp-trie "0.1.10"]]
 :repositories cemerick.pomegranate.aether/maven-central )

(import [me.geso.regexp_trie RegexpTrie])

(defn make-url-re [urls]
  (str "^"
       (-> (.regexp (reduce (fn [r v] (.add r v) r) (RegexpTrie.) urls))
           ;; quoting `.' and `/' in regexp by string/replace is workaround
           (replace #"[/.]" "\\\\$0")
           ;; JavaScript regexp doesn't handle `\Q..\E' (workaround)
           (replace "\\Q:\\E" ":") )))

(defn read-conf []
  (let [conf (edn/read-string (slurp "resources/config.edn"))]
    {:urls (concat (:shorturl-hosts conf)
                   (:htnto-hosts conf)
                   (:bitly-hosts conf) )
     :hostname (:hostname conf) }))

(defn render-template [tmpl dat]
  (reduce (fn [r v] (replace r (str "{{" (name v) "}}") (dat v)))
          tmpl
          (keys dat) ))

(defn minify-js [dat]
  ;; workaround.  Be careful about string literals
  (-> dat
      (replace #"\s//[^\n]*" "")        ; remove comments
      (replace #"\s{2,}" " ")           ; compress spaces
      (replace #" (=|==|\+|<|>|&&|\|\|) (?!\!)" "$1")
      (replace #"([,!]) " "$1")
      (replace #"([;{}]) " "$1")
      (replace " {" "{")
      (replace #"([()]) ([()])" "$1$2")
      (replace #"(if|for) \(" "$1(") ))

(defn build-js [urls hostname]
  (.mkdirs (io/file "resources/public/js"))
  (spit "resources/public/js/nagainolet.js"
        (-> (slurp "resources/templates/js/nagainolet.js")
            (render-template {:URL_RE (make-url-re urls) :HOSTNAME hostname})
            (minify-js) )))

(defn build-hosts-json [urls]
  (spit "resources/public/hosts.json"
        (json/generate-string urls) ))

(defn buildfile [project]
  (let [{urls :urls hostname :hostname} (read-conf)]
    (build-js urls hostname)
    (build-hosts-json urls) ))
