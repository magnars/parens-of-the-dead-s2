(ns ^:figwheel-hooks undead.dev
  (:require [gadget.inspector :as inspector]
            [undead.client.main :as main]))

(defn ^:after-load render []
  (main/render))

(defonce started
  (main/start))

(inspector/inspect "Store" main/store)
