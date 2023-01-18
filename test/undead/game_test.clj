(ns undead.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.game :as sut]))

(defn filter-events [event-kinds events]
  (filter #(event-kinds (first %)) events))

;; update-game

(deftest update-game--set-player-rerolls
  (is (= (sut/update-game {} [:set-player-rerolls 2])
         {:rerolls 2})))

(deftest update-game--spent-reroll
  (is (= (sut/update-game {} [:spent-reroll {:rerolls 3
                                             :spent-rerolls #{0}}])
         {:spent-rerolls #{0}})))

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
           [[:set-seed 1]]))
))
