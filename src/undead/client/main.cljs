(ns undead.client.main
  (:require [chord.client :as chord]
            [cljs.core.async :refer [<!]]
            [clojure.core.match :refer [match]]
            [dumdom.core :as d])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce store (atom {}))

(d/defcomponent Page [{:keys [zombies]}]
  [:div.page
   [:div.surface
    [:div.skyline
     (for [i (range 16)]
       [:div.building {:class (str "building-" i)}])]
    [:div.zombies
     (for [zombie (vals zombies)]
       [:div.zombie-position
        [:div.zombie {:class (:kind zombie)}]])]]])

(defn render []
  (d/render (Page @store) (js/document.getElementById "main")))

(defn start []
  (add-watch store ::me (fn [_ _ _ _] (render)))
  (go
    (let [{:keys [error ws-channel]} (<! (chord/ws-ch "ws://localhost:8666/ws"))]
      (when error
        (throw error))
      (doseq [action (:message (<! ws-channel))]
        (prn action)
        (match action
          [:assoc-in path v] (swap! store assoc-in path v))))))
