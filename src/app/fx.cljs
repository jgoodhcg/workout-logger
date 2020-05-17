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

(defn convert-user [user]
  (-> user
      (j/call :toJSON)
      (js->clj :keywordize-keys true)
      (select-keys [:email])))

(def auth-state-subscription (atom nil))

(defn auth-state-change [u]
  (println "auth state changed")
  (println u)
  (if-some [user u]
    (>evt [:load-user-success (convert-user user)])
    (>evt [:navigate :login])))

(defn auth-state-subscribe []
  (reset! auth-state-subscription
          (-> firebase
              (j/call :auth)
              (j/call :onAuthStateChanged auth-state-change))))

(reg-fx :firebase-init
        (fn [config]
          (-> firebase (.initializeApp (clj->js config)))))

(defn auth-persist [on-success on-error]
  (-> firebase
      (j/call :auth)
      (j/call :setPersistence (-> firebase
                                  (j/get :auth)
                                  (j/get :Auth)
                                  (j/get :Persistence)
                                  (j/get :LOCAL)))
      (j/call :then (clj->js on-success))
      (j/call :catch (clj->js on-error))))

(reg-fx :firebase-signup
        (fn [{:keys [email password]}]
          (auth-persist
           (fn [_]
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
                                    (println error))))))
           #(println %))))

(reg-fx :navigate
        (fn [screen]
          (j/call nav/Actions screen)))

(defn sign-in-callback [user-cred]
  (-> user-cred
      (js->clj :keywordize-keys true)
      (:user)
      (convert-user)
      (#(>evt [:login-success %]))))

(reg-fx :firebase-login
        (fn [{:keys [email password]}]
          (auth-persist
           #(-> firebase
                (j/call :auth)
                (j/call :signInWithEmailAndPassword email password)
                (j/call :then (clj->js sign-in-callback))
                (.catch (clj->js (fn [error]
                                   ;; TODO dispatch an alert event for the user
                                   (println error)))))
           #(println %))))

(reg-fx :firebase-load-user
        (fn []
          (when-some [unsubscribable @auth-state-subscription]
            (unsubscribable))
          (auth-state-subscribe)))

(reg-fx :firebase-logout
        (fn [_]
          (-> firebase
              (j/call :auth)
              (j/call :signOut))))
