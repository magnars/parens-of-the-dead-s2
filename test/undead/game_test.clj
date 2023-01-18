(ns undead.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.game :as sut]))

(defn filter-events [event-kinds events]
  (filter #(event-kinds (first %)) events))

(def faces [:punch :heal :shields :shovel :punches :skull])

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
                                                     :faces faces
                                                     :current-face 2}}}
                                     [:reroll 0])
                (filter-events #{:dice-rolled}))
           [[:dice-rolled [{:die-id :die-0
                            :from 1
                            :to 2
                            :roll-id 0}
                           {:die-id :die-1
                            :from 2
                            :to 4
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
