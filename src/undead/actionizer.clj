(ns undead.actionizer
  (:require [clojure.core.match :refer [match]]))

(defn balance-hearts [hearts]
  (let [heart-count (count hearts)
        num-lines (Math/ceil (/ heart-count 6))]
    (partition-all (/ heart-count num-lines) hearts)))

(defn prepare-zombie [zombie]
  {:kind (:kind zombie)
   :hearts (balance-hearts
            (concat (repeat (-> zombie :health :current) :heart)
                    (repeat (- (-> zombie :health :max)
                               (-> zombie :health :current)) :lost)))})

(defn add-zombie [zombie]
  [[:assoc-in [:zombies (:id zombie)] (prepare-zombie zombie)]])

(defn event->actions [event]
  (match event
    [:add-zombie zombie] (add-zombie zombie)))
