(ns undead.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.game :as sut]))

;; update-game

(deftest update-game--set-player-rerolls
  (is (= (sut/update-game {} [:set-player-rerolls 2])
         {:rerolls 2})))

(deftest update-game--spent-reroll
  (is (= (sut/update-game {} [:spent-reroll {:rerolls 3
                                             :spent-rerolls #{0}}])
         {:spent-rerolls #{0}})))

;; perform-command

(deftest perform-command--reroll
  (is (= (sut/perform-command {:rerolls 2} [:reroll 1])
         [[:spent-reroll {:rerolls 2
                          :spent-rerolls #{1}}]]))

  (is (= (sut/perform-command {:rerolls 3} [:reroll 0])
         [[:spent-reroll {:rerolls 3
                          :spent-rerolls #{0}}]]))

  (is (= (sut/perform-command {:rerolls 3
                               :spent-rerolls #{1}} [:reroll 0])
         [[:spent-reroll {:rerolls 3
                          :spent-rerolls #{0 1}}]])))
