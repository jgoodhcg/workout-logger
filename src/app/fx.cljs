(ns app.fx
  (:require
   ["firebase" :as firebase]
   ["firebase/auth"]
   ["react-native-router-flux" :as nav]
   ["expo-secure-store" :as secure-store]
   ["react-native" :as rn]
   [applied-science.js-interop :as j]
   [re-frame.core :refer [reg-fx dispatch]]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
   [clojure.spec.alpha :as s]
   [app.helpers :refer [>evt]]
   [app.fx-atoms :refer [auth auth-state-subscription]]))

(defn convert-user [user]
  (-> user
      (j/call :toJSON)
      (js->clj :keywordize-keys true)
      (select-keys [:email])))

(defn auth-state-change [u]
  (println "auth state changed")
  (println u)
  (if-some [user u]
    (>evt [:load-user-success (convert-user user)])
    (>evt [:navigate :login])))

(defn auth-state-subscribe []
  (println "auth state subscribe")
  (println @auth)
  (reset! auth-state-subscription
          (-> @auth
              (j/call :onAuthStateChanged auth-state-change))))

(reg-fx :firebase-init
        (fn [config]
          (println "firebase init")
          (-> firebase (.initializeApp (clj->js config)))
          (println @auth)
          (reset! auth (-> firebase (j/call :auth)))
          (println @auth)
          (println "firebase init end")))

(defn auth-persist [on-success on-error]
  (-> @auth
      (j/call :setPersistence (-> firebase
                                  (j/get :auth) ;; "get" not "call"
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
                                    (j/call rn/Alert :alert "Failed to signup" "Please try again")
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
           #(-> @auth
                (j/call :signInWithEmailAndPassword email password)
                (j/call :then (clj->js sign-in-callback))
                (.catch (clj->js (fn [error]
                                   (j/call rn/Alert :alert "Failed to login" "Please try again")
                                   (println error)))))
           #(println %))))

(reg-fx :firebase-load-user
        (fn []
          ;; When a callback is first registered to `onAuthStateChanged` it is called with the current auth state.
          ;; Then it is called when users are logged in or out.
          ;; Navigation is not derived from re-frame state so every hot reload will put the user on the initial page (:loading)
          ;; To get the user off the loading page we re-subscribe the `onAuthStateChanged` callback to trigger "loading the user" and instigating a navigation effect.
          ;; The unsubscribe is needed to keep callbacks from building up over multiple reloads.
          (when-some [unsubscribable @auth-state-subscription]
            (unsubscribable))
          (auth-state-subscribe)))

(reg-fx :firebase-logout
        (fn [_]
          (-> @auth
              (j/call :signOut))))

(reg-fx :firebase-send-password-reset-email
        (fn [{:keys [email]}]
          (-> @auth
              (j/call :sendPasswordResetEmail email)
              (j/call :then
                      (fn [_]
                        (j/call
                         rn/Alert :alert "📨 Reset email sent")))
              (j/call :catch
                      (fn [_]
                        (j/call
                         rn/Alert :alert "⛔ Failed to send reset email"))))))
