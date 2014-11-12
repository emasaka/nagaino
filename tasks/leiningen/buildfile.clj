(ns leiningen.buildfile
  (:use [cemerick.pomegranate :only [add-dependencies]])
  (:require [clojure.string]
            [clojure.java.io :as io] ))

(add-dependencies
 :coordinates '[[me.geso/regexp-trie "0.1.10"]]
 :repositories cemerick.pomegranate.aether/maven-central )

(import [me.geso.regexp_trie RegexpTrie])

(defn make-url-re [urls]
  (str "^"
       (-> (.regexp (reduce (fn [r v] (.add r v) r) (RegexpTrie.) urls))
           ;; quoting `.' and `/' in regexp by string/replace is workaround
           (clojure.string/replace #"[/.]" "\\\\$0")
           ;; JavaScript regexp doesn't handle `\Q..\E' (workaround)
           (clojure.string/replace "\\Q:\\E" ":") )))

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
