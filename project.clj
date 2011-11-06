(defproject nagaino "0.0.1-SNAPSHOT"
  :description "bulk expander of shortened URLs"
  :dependencies [[org.clojure/clojure "1.2.1"]
		 [org.clojure/clojure-contrib "1.2.0"]
		 [clojure-http-client/clojure-http-client "1.1.0"]
                 [compojure "0.6.5"]
                 [hiccup "0.3.6"]
                 [ring/ring-jetty-adapter "0.3.8"]
                 [congomongo "0.1.4-SNAPSHOT"] ]
  :dev-dependencies [[swank-clojure "1.3.2"]]
  :main nagaino.core )
