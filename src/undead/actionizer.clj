(ns undead.actionizer
  (:require [clojure.core.match :refer [match]]))

(defn event->actions [event]
  (match event
    [:add-zombie zombie] [[:assoc-in [:zombies (:id zombie)] zombie]]))
