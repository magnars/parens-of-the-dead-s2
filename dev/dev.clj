(ns dev
  (:require [figwheel.main.api]
            [figwheel.main]
            [integrant.repl :as repl]
            [undead.system :as system]))

(defn start []
  (set! *print-namespace-maps* false)
  (repl/set-prep! (constantly system/system))
  (repl/go))

(defn stop []
  (repl/halt))

(defn cljs []
  (if (get @figwheel.main/build-registry "dev")
    (figwheel.main.api/cljs-repl "dev")
    (figwheel.main.api/start "dev")))

(comment ;; s-:


  (start)
  (stop)
  (cljs)
  )
