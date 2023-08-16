(ns undead.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.game :as sut]))

(defn filter-events [event-kinds events]
  (filter #(event-kinds (first %)) events))

(def faces [:punch :heal :shields :shovel :punches :skull])

;; die effects

(deftest get-die-effects
  (testing "two separate punches"
    (is (= (sut/get-die-effects
            [{:id :die-0
              :faces faces
              :current-face 0}

             {:id :die-1
              :faces faces
              :current-face 0}])
           {:punches {:value 2
                      :die-ids #{:die-0 :die-1}}})))

  (testing "a separate punch and a double punch"
    (is (= (sut/get-die-effects
            [{:id :die-0
              :faces faces
              :current-face 0}

             {:id :die-1
              :faces faces
              :current-face 4}])
           {:punches {:value 3
                      :die-ids #{:die-0 :die-1}}}))))

;; update-game

(deftest update-game--start-round
  (is (= (sut/update-game {} [:started-round {:round-number 1}])
         {:round-number 1})))

(deftest update-game--set-player-rerolls
  (is (= (sut/update-game {} [:set-player-rerolls 2])
         {:rerolls 2})))

(deftest update-game--spent-reroll
  (is (= (sut/update-game {} [:spent-reroll {:rerolls 3
                                             :spent-rerolls #{0}}])
         {:spent-rerolls #{0}})))

(deftest update-game--replenished-reroll
  (is (= (sut/update-game {} [:replenished-rerolls {:rerolls 3}])
         {:spent-rerolls #{}})))

(deftest update-game--added-dice
  (testing "Adds dice where there are none"
    (is (= (:dice (sut/update-game {} [:added-dice [{:id :die-0}
                                                    {:id :die-1}]]))
           {:die-0 {:id :die-0}
            :die-1 {:id :die-1}})))

  (testing "Adds dice where there are some"
    (is (= (:dice (sut/update-game {:dice {:die-2 {:id :die-2}}}
                                   [:added-dice [{:id :die-0}
                                                 {:id :die-1}]]))
           {:die-0 {:id :die-0}
            :die-1 {:id :die-1}
            :die-2 {:id :die-2}}))))

(deftest update-game--added-zombie
  (is (= (-> (sut/update-game {} [:added-zombie {:id :zombie-0}])
             :zombies)
         {:zombie-0 {:id :zombie-0}})))

(deftest update-game--dice-rolled
  (is (= (sut/update-game {:dice {:die-0 {:id :die-0}}}
                          [:dice-rolled [{:die-id :die-0
                                          :to 3}]])
         {:dice {:die-0 {:id :die-0
                         :current-face 3}}})))

(deftest update-game--set-die-locked?
  (is (= (sut/update-game {:dice {:die-0 {:id :die-0}}}
                          [:set-die-locked? {:die-id :die-0
                                             :locked? true}])
         {:dice {:die-0 {:id :die-0 :locked? true}}})))

(deftest update-game--punched-zombie
  (is (= (sut/update-game {:zombies {:zombie-1 {:id :zombie-1
                                                :kind :biker
                                                :health {:max 8 :current 3}}}}
                          [:punched-zombie {:zombie-id :zombie-1
                                            :damage 2
                                            :die-ids #{:die-0 :die-1}
                                            :health {:max 8 :current 3}}])
         {:zombies {:zombie-1 {:id :zombie-1
                               :kind :biker
                               :health {:max 8 :current 1}}}})))

(deftest update-game--killed-zombie
  (is (empty? (-> (sut/update-game
                   {:zombies {:zombie-1 {:id :zombie-1}}}
                   [:killed-zombie :zombie-1])
                  :zombies))))

(deftest update-game--zombie-planning-meeting
  (is (-> (sut/update-game
           {:zombies {:zombie-1 {:id :zombie-1}
                      :zombie-2 {:id :zombie-2}}}
           [:zombies-planned-their-moves {:zombie-1 [:punches :punch]
                                          :zombie-2 [:punch :punch]}])
          :zombies)
      {:zombie-1 {:id :zombie-1 :intensions [:punches :punch]}
       :zombie-2 {:id :zombie-2 :intensions [:punch :punch]}}))

;; perform-command

(defn perform-command [game command]
  (sut/perform-command
   (merge {:seed 0 :round-number 1} game)
   command))

(deftest perform-command--initialize
  (testing "Sets the seed"
    (is (= (->> (perform-command {} [:initialize 666])
                (filter-events #{:set-seed}))
           [[:set-seed 667]])))

  (testing "Plans the zombie rounds"
    (is (= (->> (perform-command {} [:initialize 666])
                (filter-events #{:zombies-planned-their-moves}))
           [[:zombies-planned-their-moves
             {:zombie-1 [:punch :punch]}]]))))

(deftest perform-command--reroll
  (testing "Use reroll"
    (is (= (->> (perform-command {:rerolls 2} [:reroll 1])
                (filter-events #{:spent-reroll}))
           [[:spent-reroll {:rerolls 2
                            :spent-rerolls #{1}}]])))

  (testing "Use another reroll"
    (is (= (->> (perform-command {:rerolls 3
                                  :spent-rerolls #{1}} [:reroll 0])
                (filter-events #{:spent-reroll}))
           [[:spent-reroll {:rerolls 3
                            :spent-rerolls #{0 1}}]])))

  (testing "Can't spend used reroll"
    (is (nil? (perform-command {:rerolls 3
                                :spent-rerolls #{1}} [:reroll 1]))))

  (testing "Can't spend non-existent reroll"
    (is (nil? (perform-command {:rerolls 3
                                :spent-rerolls #{1}} [:reroll 3])))

    (is (nil? (perform-command {:rerolls 3
                                :spent-rerolls #{1}} [:reroll -1]))))

  (testing "Produces a new seed"
    (is (= (->> (perform-command {:seed 1
                                  :rerolls 3} [:reroll 0])
                (filter-events #{:set-seed}))
           [[:set-seed 2]])))

  (testing "Rolling the dice"
    (is (= (->> (perform-command {:rerolls 3
                                  :dice {:die-0 {:id :die-0
                                                 :faces faces
                                                 :current-face 1}
                                         :die-1 {:id :die-1
                                                 :locked? true
                                                 :faces faces
                                                 :current-face 2}}}
                                 [:reroll 0])
                (filter-events #{:dice-rolled}))
           [[:dice-rolled [{:die-id :die-0
                            :from 1
                            :to 2
                            :roll-id 0}]]])))

  )

(deftest perform-command--set-die-locked?
  (is (= (->> (perform-command {:dice {:die-0 {:id :die-0}}}
                               [:set-die-locked? :die-0 true])
              (filter-events #{:set-die-locked?}))
         [[:set-die-locked? {:die-id :die-0
                             :locked? true}]]))

  (is (= (->> (perform-command {:dice {:die-0 {:id :die-0}}}
                               [:set-die-locked? :die-6 true])
              (filter-events #{:set-die-locked?}))
         [])))

(deftest perform-command--finish-turn
  (testing "Punch a zombie!"
    (is (= (->> (perform-command
                 {:dice {:die-0 {:id :die-0
                                 :faces faces
                                 :current-face 0}}
                  :zombies {:zombie-0 {:id :zombie-0
                                       :kind :biker
                                       :health {:max 8 :current 6}}}}
                 [:finish-turn {:target :zombie-0}])
                (filter-events #{:punched-zombie}))
           [[:punched-zombie {:zombie-id :zombie-0
                              :damage 1
                              :die-ids #{:die-0}
                              :health {:max 8 :current 6}}]])))

  (testing "Cannot punch non-existent zombie"
    (is (= (->> (perform-command
                 {:dice {:die-0 {:id :die-0
                                 :faces faces
                                 :current-face 0}}}
                 [:finish-turn {:target :zombie-0}])
                (filter-events #{:punched-zombie}))
           [])))

  (testing "Cannot remove life that is not there"
    (is (= (->> (perform-command
                 {:dice {:die-0 {:id :die-0
                                 :faces faces
                                 :current-face 0}
                         :die-1 {:id :die-1
                                 :faces faces
                                 :current-face 4}}
                  :zombies {:zombie-1 {:id :zombie-1
                                       :kind :biker
                                       :health {:max 8 :current 2}}}}
                 [:finish-turn {:target :zombie-1}])
                (filter-events #{:punched-zombie :killed-zombie}))
           [[:punched-zombie {:zombie-id :zombie-1
                              :damage 2
                              :die-ids #{:die-0 :die-1}
                              :health {:max 8 :current 2}}]
            [:killed-zombie :zombie-1]])))

  (testing "Unlocks locked dice in the game"
    (is (= (->> (perform-command
                 {:dice {:die-0 {:id :die-0
                                 :faces faces
                                 :locked? true
                                 :current-face 0}
                         :die-1 {:id :die-1
                                 :faces faces
                                 :current-face 4}}}
                 [:finish-turn {:target :zombie-1}])
                (filter-events #{:set-die-locked?}))
           [[:set-die-locked? {:die-id :die-0
                               :locked? false}]])))

  (testing "Rerolls the dice"
    (is (= (->> (perform-command
                 {:seed 1
                  :dice {:die-0 {:id :die-0
                                 :faces faces
                                 :locked? true
                                 :current-face 0}
                         :die-1 {:id :die-1
                                 :faces faces
                                 :current-face 4}}}
                 [:finish-turn {:target :zombie-1}])
                (filter-events #{:dice-rolled :set-seed}))
           [[:dice-rolled [{:die-id :die-0
                            :from 0
                            :to 3
                            :roll-id 1}
                           {:die-id :die-1
                            :from 4
                            :to 2
                            :roll-id 1}]]
            [:set-seed 2]])))

  (testing "Gimme back my rerolls!"
    (is (= (->> (perform-command
                 {:rerolls 3
                  :dice {:die-0 {:id :die-0
                                 :faces faces
                                 :locked? true
                                 :current-face 0}
                         :die-1 {:id :die-1
                                 :faces faces
                                 :current-face 4}}}
                 [:finish-turn {:target :zombie-1}])
                (filter-events #{:replenished-rerolls}))
           [[:replenished-rerolls {:rerolls 3}]])))

  (testing "Starts new round"
    (is (= (->> (perform-command
                 {:rerolls 3 :round-number 2}
                 [:finish-turn {:target :zombie-1}])
                (filter-events #{:started-round}))
           [[:started-round {:round-number 3}]])))

  (testing "Replans the zombies"
    (is (= (->> (perform-command
                 {:round-number 3
                  :zombies {:zombie-1
                            {:id :zombie-1
                             :kind :biker
                             :behaviour {:strategy :round-number
                                         :actions [[:punch :punch]
                                                   [:punches :punch]]}
                             :health {:max 8 :current 8}}}}
                 [:finish-turn {:target :zombie-1}])
                (filter-events #{:zombies-planned-their-moves}))
           [[:zombies-planned-their-moves {:zombie-1 [:punches :punch]}]]))))
