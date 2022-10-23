(ns undead.actionizer
  (:require [clojure.core.match :refer [match]]))

(defn balance-hearts [hearts]
  (let [heart-count (count hearts)
        num-lines (Math/ceil (/ heart-count 6))]
    (partition-all (/ heart-count num-lines) hearts)))

(defn render-hearts [health]
  (concat (repeat (:current health) :heart)
          (repeat (- (:max health)
                     (:current health)) :lost)))

(defn prepare-zombie [zombie]
  {:kind (:kind zombie)
   :hearts (balance-hearts
            (render-hearts (:health zombie)))})

(defn add-zombie [zombie]
  [[:assoc-in [:zombies (:id zombie)] (prepare-zombie zombie)]])

(defn set-player-health [health]
  [[:assoc-in [:player :hearts] (render-hearts health)]])

(defn add-dice [dice]
  (for [die dice]
    [:assoc-in [:dice (:id die)] die]))

(defn event->actions [event]
  (match event
    [:added-dice dice] (add-dice dice)
    [:added-zombie zombie] (add-zombie zombie)
    [:set-player-health health] (set-player-health health)))
