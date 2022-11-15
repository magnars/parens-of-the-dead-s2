(ns undead.game-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.game :as sut]))

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
