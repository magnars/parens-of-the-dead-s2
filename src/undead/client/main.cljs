(ns undead.client.main
  (:require [dumdom.core :as d]))

(d/defcomponent Page [props]
  [:div.page
   [:div.surface
    [:div.skyline
     (for [i (range 16)]
       [:div.building {:class (str "building-" i)}])]]])

(defn render []
  (d/render (Page {}) (js/document.getElementById "main")))

(print "Hello from Maine!")
