(ns undead.game-loop
  (:require [clojure.core.async :refer [<! go put!]]
            [undead.actionizer :as actionizer]
            [undead.game :as game]))

(defn perform-command [game command]
  (try
    (let [events (game/perform-command game command)
          new-game (reduce game/update-game game events)
          actions (mapcat actionizer/event->actions events)]
      [new-game actions])
    (catch Exception e
      [nil [[:assoc-in [:error] (pr-str e)]]])))

(defn start! [ws-channel]
  (let [[new-game actions] (perform-command {} [:initialize (System/currentTimeMillis)])]
    (put! ws-channel actions)
    (go
      (loop [game new-game]
        (let [command (:message (<! ws-channel))
              [updated-game actions] (perform-command game command)]
          (put! ws-channel actions)
          (when (and updated-game (not (:game-over? updated-game)))
            (recur updated-game)))))))
