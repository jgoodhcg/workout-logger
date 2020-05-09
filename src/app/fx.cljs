(ns app.fx
  (:require
   ["firebase" :as firebase]
   ["firebase/auth"]
   ["react-native-router-flux" :as nav]
   ["expo-secure-store" :as secure-store]
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

(defn sign-in-callback [user-cred]
  (-> user-cred
      (js->clj :keywordize-keys true)
      (:user) ;; :credential here is always nil ???
      ;; propbably because it is just email/pass
      (j/call :toJSON)
      (js->clj :keywordize-keys true)
      (#(>evt [:login-success %])))

  ;; put email/pass in secure store to log in again
  ;; this isn't great but setPersistence()
  ;; doesn't seem to work ):
  (j/call secure-store :setItemAsync
          "credential"
          (-> firebase
              (j/get :auth)
              (j/get :EmailAuthProvider)
              (j/call :credential email password)
              (j/call :toJSON)
              (->> (j/call js/JSON :stringify)))))

(reg-fx :firebase-login
        (fn [{:keys [email password]}]
          (-> firebase
              (j/call :auth)
              (j/call :signInWithEmailAndPassword email password)
              (j/call :then (clj->js sign-in-callback))

              (.catch (clj->js (fn [error]
                                 ;; TODO dispatch an alert event for the user
                                 (println error)))))))

(reg-fx :firebase-load-user
        (fn []
          (-> secure-store
              (j/call :getItemAsync "credential")
              (j/call
               :then
               (clj->js (fn [credential]
                          (println "pulled out credential from secure store")
                          (println credential)
                          (-> firebase
                              (j/call :auth)
                              (j/call :signInWithCredential
                                      (-> firebase
                                          (j/get :auth)
                                          (j/get :AuthCredential)
                                          (j/call :fromJSON credential)))
                              (j/call :then (clj->js sign-in-callback)))))))))
