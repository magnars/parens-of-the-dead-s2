(ns undead.game
  (:require [clojure.core.match :refer [match]]))

(defn get-initial-events [seed]
  (let [rng (java.util.Random. seed)]
    [[:added-zombie {:id :zombie-1
                     :kind :biker
                     :health {:max 8 :current 6}}]
     [:set-player-health {:max 9 :current 9}]
     [:added-dice (for [i (range 5)]
                    {:id (keyword (str "die-" i))
                     :faces [:punch :heal :shields :shovel :punches :skull]
                     :current-face (mod (.nextInt rng) 6)})]
     [:set-player-rerolls 2]]))

(defn update-game [game event]
  (match event
    [:added-dice dice] game
    [:added-zombie zombie] game
    [:set-player-health health] game
    [:set-player-rerolls n] (assoc game :rerolls n)
    [:spent-reroll opt] (assoc game :spent-rerolls (:spent-rerolls opt))))

(defn reroll [game n]
  [[:spent-reroll {:rerolls (:rerolls game)
                   :spent-rerolls (conj (:spent-rerolls game #{}) n)}]])

(defn perform-command [game command]
  (match command
    [:reroll n] (reroll game n)))
