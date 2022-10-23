(ns undead.game)

(def initial-events
  [[:added-zombie {:id :zombie-1
                   :kind :biker
                   :health {:max 8 :current 6}}]
   [:set-player-health {:max 9 :current 9}]
   [:added-dice [{:faces [:punch :heal :shields :shovel :punches :skull]
                  :current-face 2}]]])

