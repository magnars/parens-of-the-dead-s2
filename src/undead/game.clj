(ns undead.game
  (:require [clojure.core.match :refer [match]]))

(def zombies
  [{:id :zombie-1
    :kind :biker
    :behaviour {:strategy :round-number
                :actions [[:punch :punch]
                          [:punches :punch]]}
    :health {:max 8 :current 8}}])

(defn current-face [die]
  (nth (:faces die) (:current-face die)))

(defn punch-value [action]
  (case action
    :punch 1
    :punches 2
    0))

(defn get-die-effects [dice]
  {:punches (let [punch-dice (filter (comp #{:punch :punches} current-face) dice)]
              {:value (apply + (map (comp punch-value current-face) punch-dice))
               :die-ids (set (map :id punch-dice))})})

(defn get-initial-events [seed]
  (let [rng (java.util.Random. seed)]
    [[:added-zombie (first zombies)]
     [:set-player-health {:max 9 :current 9}]
     [:added-dice (for [i (range 5)]
                    {:id (keyword (str "die-" i))
                     :faces [:punch :heal :shields :shovel :punches :skull]
                     :current-face (mod (.nextInt rng) 6)})]
     [:set-player-rerolls 2]
     [:set-seed (inc seed)]]))

(defn add-dice [game dice]
  (update game :dice (fnil into {}) (map (juxt :id identity) dice)))

(defn roll-die [game roll]
  (assoc-in game [:dice (:die-id roll) :current-face] (:to roll)))

(defn punch-zombie [game {:keys [zombie-id damage]}]
  (update-in game [:zombies zombie-id :health :current] - damage))

(defn update-zombie-intentions [game id->intentions]
  (reduce (fn [game [id intentions]]
            (assoc-in game [:zombies id :intentions] intentions))
          game
          id->intentions))

(defn punch-player [game opts]
  (update-in game [:player :health :current] - (:damage opts)))

(defn update-game [game event]
  (match event
    [:added-dice dice] (add-dice game dice)
    [:added-zombie zombie] (assoc-in game [:zombies (:id zombie)] zombie)
    [:dice-rolled rolls] (reduce roll-die game rolls)
    [:killed-player] {:game-over? true}
    [:killed-zombie zombie-id] (update game :zombies dissoc zombie-id)
    [:punched-player opts] (punch-player game opts)
    [:punched-zombie opts] (punch-zombie game opts)
    [:replenished-rerolls opts] (assoc game :spent-rerolls #{})
    [:set-die-locked? opts] (assoc-in game [:dice (:die-id opts) :locked?] (:locked? opts))
    [:set-player-health health] (assoc-in game [:player :health] health)
    [:set-player-rerolls n] (assoc game :rerolls n)
    [:set-seed seed] (assoc game :seed seed)
    [:spent-reroll opt] (assoc game :spent-rerolls (:spent-rerolls opt))
    [:started-round opt] (assoc game :round-number (:round-number opt))
    [:zombies-planned-their-moves opt] (update-zombie-intentions game opt)
    ))

(defn reroll-allowed? [{:keys [rerolls spent-rerolls]} n]
  (and
   (< -1 n rerolls)
   (not (contains? (set spent-rerolls) n))))

(defn roll-dice [game rng dice]
  [:dice-rolled (for [die dice]
                  {:die-id (:id die)
                   :from (:current-face die)
                   :to (mod (.nextInt rng) (count (:faces die)))
                   :roll-id (:seed game)})])

(defn reroll [game n]
  (when (reroll-allowed? game n)
    (let [rng (java.util.Random. (:seed game))]
      [[:spent-reroll {:rerolls (:rerolls game)
                       :spent-rerolls (conj (:spent-rerolls game #{}) n)}]
       (roll-dice game rng (->> (vals (:dice game))
                                (remove :locked?)))
       [:set-seed (inc (:seed game))]])))

(defn set-die-locked? [game die-id locked?]
  (when (get-in game [:dice die-id])
    [[:set-die-locked? {:die-id die-id, :locked? locked?}]]))

(defn deliver-package-of-punches [game target]
  (when-let [zombie (get-in game [:zombies target])]
    (let [{:keys [punches]} (get-die-effects (vals (:dice game)))
          damage (min (:value punches)
                      (:current (:health zombie)))]
      (cond-> [[:punched-zombie {:zombie-id (:id zombie)
                                 :damage damage
                                 :die-ids (:die-ids punches)
                                 :health (:health zombie)}]]
        (= damage (:current (:health zombie)))
        (conj [:killed-zombie target])))))

(defn unlock-dice [game]
  (->> (vals (:dice game))
       (filter :locked?)
       (mapcat #(set-die-locked? game (:id %) false))))

(defn zombie-planning-meeting [game round-number]
  [[:zombies-planned-their-moves
    (->> (for [zombie (vals (:zombies game))]
           [(:id zombie) (when-let [actions (get-in zombie [:behaviour :actions])]
                           (get-in actions [(mod (dec round-number) (count actions))]))])
         (into {}))]])

(defn start-round [game]
  (let [round-number (inc (:round-number game 0))]
    (concat
     (zombie-planning-meeting game round-number)
     [[:started-round {:round-number round-number}]])))

(defn get-intention-effects [intentions]
  (when-let [punches (seq (filter #{:punch :punches} intentions))]
    {:punches {:value (apply + (map punch-value punches))}}))

(defn get-zombie-turn-events [game zombie]
  (mapcat
   (fn [[kind opts]]
     (case kind
       :punches
       (cond-> [[:punched-player {:damage (min (:value opts)
                                               (get-in game [:player :health :current]))
                                  :health (get-in game [:player :health])}]]
         (<= (get-in game [:player :health :current]) (:value opts))
         (conj [:killed-player]))))
   (get-intention-effects (:intentions zombie))))

(defn perform-zombie-turns [game]
  (:events
   (reduce (fn [{:keys [events game]} zombie]
             (let [zombie-events (get-zombie-turn-events game zombie)]
               {:game (reduce update-game game zombie-events)
                :events (into events zombie-events)}))
           {:game game
            :events []}
           (vals (:zombies game)))))

(defn complete-round [game]
  (let [rng (java.util.Random. (:seed game))]
    (concat
     (unlock-dice game)
     [[:replenished-rerolls (select-keys game [:rerolls])]]
     [(roll-dice game rng (vals (:dice game)))]
     [[:set-seed (inc (:seed game))]])))

(defn finish-turn [game {:keys [target]}]
  (let [dice-events (deliver-package-of-punches game target)
        game (reduce update-game game dice-events)]
    (concat dice-events
            (perform-zombie-turns game)
            (complete-round game)
            (start-round game))))

(defn initialize-game [seed]
  (let [events (get-initial-events seed)
        game (reduce update-game {} events)]
    (concat events
            (start-round game))))

(defn perform-command [game command]
  (match command
    [:finish-turn opts] (finish-turn game opts)
    [:initialize seed] (initialize-game seed)
    [:reroll n] (reroll game n)
    [:set-die-locked? die-id locked?] (set-die-locked? game die-id locked?)))
