(ns undead.client.components
  (:require [dumdom.core :as d]))

(d/defcomponent Page [{:keys [zombies error player dice]}]
  (if error
    [:div.page [:pre error]]
    [:div.page
     [:div.surface
      [:div.skyline
       (for [i (range 16)]
         [:div.building {:class (str "building-" i)}])]
      [:div.zombies
       (for [zombie (vals zombies)]
         [:div.zombie-position
          [:div.zombie {:class (:kind zombie)}
           [:div.zombie-health
            (for [line (:hearts zombie)]
              [:div (for [heart line]
                      [:div.heart {:class heart}])])]]])]
      [:div.player-health
       (for [heart (:hearts player)]
         [:div {:class heart}])]
      [:div.dice-row
       (interpose
        [:div.dice-spacing]
        (for [die (vals dice)]
          [:div.die-w-lock
           [:div.die {:class (:status die)}
            [:div.cube {:class (str "entering-" (:current-face die))}
             (map-indexed
              (fn [i face]
                [:div.face {:class [(str "face-" i)
                                    face]}])
              (:faces die))]]]))]]]))
