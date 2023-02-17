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
   :on-click [:finish-turn {:target (:id zombie)}]
   :hearts (balance-hearts
            (render-hearts (:health zombie)))})

(defn add-zombie [zombie]
  [[:assoc-in [:zombies (:id zombie)] (prepare-zombie zombie)]])

(defn set-player-health [health]
  [[:assoc-in [:player :hearts] (render-hearts health)]])

(defn render-faces [faces]
  (map-indexed
   (fn [i face]
     [(str "face-" i) face])
   faces))

(defn add-dice [dice]
  (concat
   (for [die dice]
     [:assoc-in [:dice (:id die)]
      {:faces (render-faces (:faces die))
       :lock-command [:set-die-locked? (:id die) true]
       :die-class "entering"
       :cube-class (str "entering-" (:current-face die))}])
   [[:wait 1800]]))

(defn prepare-rerolls [opts]
  [[:assoc-in [:player :rerolls]
    (for [i (range (:rerolls opts))]
      (if (contains? (:spent-rerolls opts) i)
        {:used? true}
        {:on-click [:reroll i]}))]])

(defn roll-dice [rolls]
  (concat
   (mapcat
    (fn [{:keys [die-id from to roll-id]}]
      [[:assoc-in [:dice die-id :die-class] "rolling"]
       [:assoc-in [:dice die-id :cube-class] (str "roll-" from "-to-" to)]
       [:assoc-in [:dice die-id :key] (str (name die-id) "-" roll-id)]])
    rolls)
   [[:wait 1800]]))

(defn set-die-locked? [{:keys [die-id locked?]}]
  [[:assoc-in [:dice die-id :clamp-class] (when locked? "locked")]
   [:assoc-in [:dice die-id :lock-command] [:set-die-locked? die-id (not locked?)]]])

(defn event->actions [event]
  (match event
    [:added-dice dice] (add-dice dice)
    [:added-zombie zombie] (add-zombie zombie)
    [:dice-rolled rolls] (roll-dice rolls)
    [:set-die-locked? opts] (set-die-locked? opts)
    [:set-player-health health] (set-player-health health)
    [:set-player-rerolls n] (prepare-rerolls {:rerolls n})
    [:set-seed seed] nil
    [:spent-reroll opts] (prepare-rerolls opts)))
