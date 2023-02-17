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

(deftest update-game--set-player-rerolls
  (is (= (sut/update-game {} [:set-player-rerolls 2])
         {:rerolls 2})))

(deftest update-game--spent-reroll
  (is (= (sut/update-game {} [:spent-reroll {:rerolls 3
                                             :spent-rerolls #{0}}])
         {:spent-rerolls #{0}})))

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

;; perform-command

(deftest perform-command--initialize
  (is (= (->> (sut/perform-command {} [:initialize 666])
              (filter-events #{:set-seed}))
         [[:set-seed 667]])))

(deftest perform-command--reroll
  (testing "Use reroll"
    (is (= (->> (sut/perform-command {:seed 0, :rerolls 2} [:reroll 1])
                (filter-events #{:spent-reroll}))
           [[:spent-reroll {:rerolls 2
                            :spent-rerolls #{1}}]])))

  (testing "Use another reroll"
    (is (= (->> (sut/perform-command {:seed 0
                                      :rerolls 3
                                      :spent-rerolls #{1}} [:reroll 0])
                (filter-events #{:spent-reroll}))
           [[:spent-reroll {:rerolls 3
                            :spent-rerolls #{0 1}}]])))

  (testing "Can't spend used reroll"
    (is (nil? (sut/perform-command {:seed 0
                                    :rerolls 3
                                    :spent-rerolls #{1}} [:reroll 1]))))

  (testing "Can't spend non-existent reroll"
    (is (nil? (sut/perform-command {:seed 0
                                    :rerolls 3
                                    :spent-rerolls #{1}} [:reroll 3])))

    (is (nil? (sut/perform-command {:seed 0
                                    :rerolls 3
                                    :spent-rerolls #{1}} [:reroll -1]))))

  (testing "Produces a new seed"
    (is (= (->> (sut/perform-command {:seed 0
                                      :rerolls 3} [:reroll 0])
                (filter-events #{:set-seed}))
           [[:set-seed 1]])))

  (testing "Rolling the dice"
    (is (= (->> (sut/perform-command {:seed 0
                                      :rerolls 3
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
  (is (= (->> (sut/perform-command {:dice {:die-0 {:id :die-0}}}
                                   [:set-die-locked? :die-0 true])
              (filter-events #{:set-die-locked?}))
         [[:set-die-locked? {:die-id :die-0
                             :locked? true}]]))

  (is (= (->> (sut/perform-command {:dice {:die-0 {:id :die-0}}}
                                   [:set-die-locked? :die-6 true])
              (filter-events #{:set-die-locked?}))
         [])))

(deftest perform-command--finish-turn
  (testing "Punch a zombie!"
    (is (= (->> (sut/perform-command
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
    (is (= (->> (sut/perform-command
                 {:dice {:die-0 {:id :die-0
                                 :faces faces
                                 :current-face 0}}}
                 [:finish-turn {:target :zombie-0}])
                (filter-events #{:punched-zombie}))
           [])))

  (testing "Cannot remove life that is not there"
    (is (= (->> (sut/perform-command
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
    (is (= (->> (sut/perform-command
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
                               :locked? false}]]))))
