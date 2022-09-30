(ns undead.actionizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.actionizer :as sut]))

(deftest actionize--add-zombie
  (is (= (sut/event->actions [:add-zombie {:id :zombie-1}])
         [[:assoc-in [:zombies :zombie-1] {:id :zombie-1}]])))
