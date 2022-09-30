(ns undead.system
  (:require [clojure.java.io :as io]
            [compojure.core :refer [GET routes]]
            [compojure.route :as route]
            [integrant.core :as ig]
            [org.httpkit.server :as server]))

(def system
  {:app/config {}

   :app/handler {}

   :app/adapter {:config (ig/ref :app/config)
                 :handler (ig/ref :app/handler)}})

(defmethod ig/init-key :app/config [_ _]
  (read-string (slurp (io/resource "config.edn"))))

(defmethod ig/init-key :app/handler [_ _]
  (routes
   (GET "/" [] (io/resource "public/index.html"))
   (route/resources "/")))

(defmethod ig/init-key :app/adapter [_ {:keys [config handler]}]
  (server/run-server handler {:port (:port config)}))

(defmethod ig/halt-key! :app/adapter [_ stop]
  (stop))

(comment
  (read-string (slurp (io/resource "config.edn")))

  )
