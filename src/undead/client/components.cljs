(ns undead.client.components
  (:require [dumdom.core :as d]))

(d/defcomponent Zombie [{:keys [kind hearts]}]
  [:div.zombie-position
   [:div.zombie {:class kind}
    [:div.zombie-health
     (for [line hearts]
       [:div (for [heart line]
               [:div.heart {:class heart}])])]]])

(d/defcomponent DieWithLock [{:keys [die-class faces cube-class]}]
  [:div.die-w-lock
   [:div.die {:class die-class}
    [:div.cube {:class cube-class}
     (for [face faces]
       [:div.face {:class face}])]]])

(d/defcomponent Page [{:keys [zombies error player dice]}]
  (if error
    [:div.page [:pre error]]
    [:div.page
     [:div.surface
      [:div.skyline
       (for [i (range 16)]
         [:div.building {:class (str "building-" i)}])]
      [:div.zombies
       (map Zombie (vals zombies))]
      [:div.player-health
       (for [heart (:hearts player)]
         [:div {:class heart}])]
      [:div.dice-row
       (interpose
        [:div.dice-spacing]
        (map DieWithLock (vals dice)))
       [:div.rerolls
        (for [{:keys [on-click]} (:rerolls player)]
          [:div.reroll {:on-click on-click}])]]]]))
