(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.2.1"]
		 [org.clojure/clojure-contrib "1.2.0"]
		 [clojure-http-client/clojure-http-client "1.1.0"]
                 [compojure "1.0.1"]
                 [hiccup "0.3.8"]
                 [ring "1.0.2"]
                 [congomongo "0.1.7"] ]
  :dev-dependencies [[swank-clojure "1.3.4"]]
  :main nagaino.core )
