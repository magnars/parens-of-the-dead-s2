(ns undead.client.components
  (:require [dumdom.core :as d]))

(d/defcomponent Zombie [{:keys [kind on-click hearts class intention-classes]}]
  [:div.zombie-position
   [:div.zombie {:class [kind class]
                 :on-click on-click}
    [:div.zombie-health
     (for [line hearts]
       [:div (for [heart line]
               [:div.heart {:class heart}])])]
    [:div.zombie-punches
     (for [i (range 1 6)]
       [:div {:class (str "zombie-punch-" i)}])]]

   (when (seq intention-classes)
     [:div.thought-bubble {:mounted-style {:opacity 0.9 :transition "opacity 2000ms"}
                           :leaving-style {:opacity 0 :transition "opacity 500ms"}}
      [:div {:class (str "intentions-" (count intention-classes))}
       (for [class intention-classes]
         [:div.intention {:class class}])]])])

(d/defcomponent DieWithLock
  [{:keys [die-class faces cube-class key clamp-class lock-command]}]
  [:div.die-w-lock
   [:div.die {:key key
              :class die-class}
    [:div.cube {:class cube-class}
     (for [face faces]
       [:div.face {:class face}])]]
   [:div.clamp {:class clamp-class}
    [:div.lock {:on-click lock-command}
     [:div.padlock]]]])

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
        (for [{:keys [on-click used?]} (:rerolls player)]
          [:div.reroll {:on-click on-click
                        :class (when used? "used")}])]]]]))
