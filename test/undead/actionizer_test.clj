(ns undead.actionizer-test
  (:require [clojure.test :refer [deftest is testing]]
            [undead.actionizer :as sut]))

(deftest actionize--added-zombie
  (is (= (sut/event->actions
          [:added-zombie {:id :zombie-1
                          :kind :biker
                          :health {:max 4 :current 4}}])
         [[:assoc-in [:zombies :zombie-1]
           {:kind :biker
            :hearts [[:heart :heart :heart :heart]]}]]))

  (is (= (sut/event->actions
          [:added-zombie {:id :zombie-1
                          :kind :biker
                          :health {:max 4 :current 3}}])
         [[:assoc-in [:zombies :zombie-1]
           {:kind :biker
            :hearts [[:heart :heart :heart :lost]]}]]))

  (is (= (sut/event->actions
          [:added-zombie {:id :zombie-1
                          :kind :biker
                          :health {:max 9 :current 8}}])
         [[:assoc-in [:zombies :zombie-1]
           {:kind :biker
            :hearts [[:heart :heart :heart :heart :heart]
                     [:heart :heart :heart :lost]]}]])))

(deftest actionize--set-player-health
  (is (= (sut/event->actions
          [:set-player-health {:max 3, :current 3}])
         [[:assoc-in [:player :hearts] [:heart :heart :heart]]]))

  (is (= (sut/event->actions
          [:set-player-health {:max 3, :current 2}])
         [[:assoc-in [:player :hearts] [:heart :heart :lost]]])))

(deftest actionize--added-dice
  (is (= (sut/event->actions
          [:added-dice [{:id :die-1
                         :faces [:punch :heal :shields :shovel :punches :skull]
                         :current-face 2}]])
         [[:assoc-in [:dice :die-1] {:faces [["face-0" :punch]
                                             ["face-1" :heal]
                                             ["face-2" :shields]
                                             ["face-3" :shovel]
                                             ["face-4" :punches]
                                             ["face-5" :skull]]
                                     :die-class "entering"
                                     :cube-class "entering-2"}]
          [:wait 1800]])))

(deftest actionize--set-player-rerolls
  (is (= (sut/event->actions
          [:set-player-rerolls 2])
         [[:assoc-in [:player :rerolls] [{:on-click [:reroll 0]}
                                         {:on-click [:reroll 1]}]]])))
