(ns undead.system
  (:require [chord.http-kit :as chord]
            [clojure.java.io :as io]
            [compojure.core :refer [GET routes]]
            [compojure.route :as route]
            [integrant.core :as ig]
            [org.httpkit.server :as server]
            [undead.game-loop :as game-loop]))

(def system
  {:app/config {}

   :app/handler {}

   :app/adapter {:config (ig/ref :app/config)
                 :handler (ig/ref :app/handler)}})

(defmethod ig/init-key :app/config [_ _]
  (read-string (slurp (io/resource "config.edn"))))

(defn ws-handler [req]
  (chord/with-channel req ws-channel
    (game-loop/start! ws-channel)))

(defmethod ig/init-key :app/handler [_ _]
  (routes
   (GET "/" [] (io/resource "public/index.html"))
   (GET "/ws" [] ws-handler)
   (route/resources "/")))

(defmethod ig/init-key :app/adapter [_ {:keys [config handler]}]
  (server/run-server handler {:port (:port config)}))

(defmethod ig/halt-key! :app/adapter [_ stop]
  (stop))

(comment
  (read-string (slurp (io/resource "config.edn")))

  )
