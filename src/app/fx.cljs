(ns app.fx
  (:require
   ["firebase" :as firebase]
   ["firebase/auth"]
   ["react-native-router-flux" :as nav]
   [applied-science.js-interop :as j]
   [re-frame.core :refer [reg-fx dispatch]]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
   [clojure.spec.alpha :as s]
   [app.helpers :refer [>evt]]))

(reg-fx :firebase-signup
        (fn [{:keys [email password]}]
          (println "firebase-signup")
          (-> firebase
              (.auth)
              (.createUserWithEmailAndPassword email password)
              (.then (clj->js (fn [user-cred]
                                (-> user-cred
                                    (js->clj :keywordize-keys true)
                                    (:user)
                                    (.toJSON)
                                    (js->clj :keywordize-keys true)
                                    (#(>evt [:signup-success %]))))))
              (.catch (clj->js (fn [error]
                                 ;; TODO dispatch an alert event for the user
                                 (println error)))))))

(reg-fx :firebase-init
        (fn [config]
          (-> firebase (.initializeApp (clj->js config)))))

(reg-fx :navigate
        (fn [screen]
          (j/call nav/Actions screen)))

(reg-fx :login
        (fn [{:keys [email password]}]
          (-> firebase
              (.auth)
              (.signInWithEmailAndPassword email password)
              ;; TODO .then with a dispatch to add user credentials to app-db
              ;; https://firebase.google.com/docs/reference/js/firebase.auth#usercredential
              (.then (clj->js (fn [user-cred]
                                (-> user-cred
                                    (js->clj :keywordize-keys true)
                                    (:user)
                                    (.toJSON)
                                    (js->clj :keywordize-keys true)
                                    (#(>evt [:login-success %]))))))
              (.catch (clj->js (fn [error]
                                 ;; TODO dispatch an alert event for the user
                                 (println error)))))))
