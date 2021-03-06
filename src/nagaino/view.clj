(ns nagaino.view
  (:require [compojure.core :refer :all]
            [hiccup.page :refer [html5 include-css]]
            [hiccup.util :refer [escape-html]]
            [clojure.string :refer [join]] ))

(defn format-html-data [sq]
  (map (fn [item]
	 [:div
	  [:table.result_item
	   [:tr
	    [:td.title "short_url"]
	    [:td.data (escape-html (:short_url item))] ]
	   [:tr
	    [:td.title "long_url"]
	    [:td.data (escape-html (:long_url item))] ]
	   [:tr
	    [:td.title "long_url_path"]
	    [:td.data (escape-html (join " ← " (:long_url_path item)))] ]
	   (when (:error item)
	     [:tr
	      [:td.title "error"]
	      [:td.data (escape-html (:error item))] ] ) ]])
       sq ))

(defn format-html [sq]
  (html5
   [:head
    [:title "result"]
    (include-css "/css/nagaino.css") ]
   [:body
    [:header [:h1 "nagaino"]]
    [:article
     [:h2 "result"]
     (format-html-data sq) ]]))
