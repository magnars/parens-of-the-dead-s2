(ns undead.game-loop
  (:require [clojure.core.async :refer [<! go put!]]
            [undead.actionizer :as actionizer]
            [undead.game :as game]))

(defn start! [ws-channel]
  (try
    (let [initial-events (game/get-initial-events (System/currentTimeMillis))
          initial-game (reduce game/update-game {} initial-events)]
      (put! ws-channel (mapcat actionizer/event->actions initial-events))
      (go
        (loop [game initial-game]
          (let [command (:message (<! ws-channel))
                events (game/perform-command game command)]
            (put! ws-channel (mapcat actionizer/event->actions events))
            (recur (reduce game/update-game game events))))))
    (catch Exception e
      [[:assoc-in [:error] (pr-str e)]])))
