(ns leiningen.buildfile
  (:refer-clojure :exclude [replace])
  (:use [cemerick.pomegranate :only [add-dependencies]]
        [clojure.string :only [replace]] )
  (:require [clojure.java.io :as io]) )

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

(defn read-url-conf []
  (let [conf (read-string (slurp "resources/config.clj"))]
    (into (:shorturl-hosts conf) (:bitly-hosts conf)) ))

(defn apply-template [tmpl dat]
  ;; workaround
  (replace tmpl "{{URL_RE}}" (:URL_RE dat)) )

(defn minify-js [dat]
  ;; workaround.  Be careful about string literals
  (-> dat
      (replace #" //[^\n]*" "")         ; remove comments
      (replace #"\s{2,}" " ")           ; compress spaces
      (replace #" (=|==|\+|<|>|&&|\|\|) (?!\!)" "$1")
      (replace #"([,!]) " "$1")
      (replace #"([;{}]) " "$1")
      (replace " {" "{")
      (replace #"([()]) ([()])" "$1$2")
      (replace #"(if|for) \(" "$1(") ))

(defn build-js []
  (.mkdirs (io/file "resources/public/js"))
  (spit "resources/public/js/nagainolet.js"
        (-> (slurp "resources/templates/js/nagainolet.js")
            (apply-template {:URL_RE (make-url-re (read-url-conf))})
            (minify-js) )))

(defn buildfile [project]
  (build-js) )
