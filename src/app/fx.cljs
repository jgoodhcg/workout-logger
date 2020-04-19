(ns app.fx
  (:require
   ["firebase" :as firebase]
   ["firebase/auth"]
   [re-frame.core :refer [reg-fx dispatch]]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
   [clojure.spec.alpha :as s]))

(reg-fx :firebase-signup
        (fn [{:keys [email password]}]
          (-> firebase
              (.auth)
              (.createUserWithEmailAndPassword email password)
              (.catch (clj->js (fn [error]
                                 ;; TODO dispatch an alert event for the user
                                 (println error)))))))

(reg-fx :firebase-init
        (fn [config]
          (-> firebase (.initializeApp (clj->js config)))))

