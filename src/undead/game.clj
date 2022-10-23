(ns undead.game)

(def initial-events
  [[:add-zombie {:id :zombie-1
                 :kind :biker
                 :health {:max 8 :current 6}}]
   [:set-player-health {:max 9 :current 9}]])

