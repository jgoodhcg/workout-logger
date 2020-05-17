(ns app.handlers
  (:require
   [re-frame.core :refer [reg-event-db
                          ->interceptor
                          reg-event-fx
                          reg-fx
                          dispatch
                          debug]]
   [com.rpl.specter :as sp :refer [select
                                   select-one
                                   setval
                                   transform
                                   selected-any?]]
   [clojure.spec.alpha :as s]
   [app.db :as db :refer [default-app-db app-db-spec]]))

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db event]
  (when-not (s/valid? spec db)
    (let [explanation (s/explain-str spec db)]
      (throw (str "Spec check failed: " explanation))
      true)))

(defn validate-spec [context]
  (let [db     (-> context :effects :db)
        old-db (-> context :coeffects :db)
        event  (-> context :coeffects :event)]

    (println event)
    (if (some? (check-and-throw app-db-spec db event))
      (assoc-in context [:effects :db] old-db)
      ;; put the old db back as the new db when check fails
      ;; otherwise return context unchanged
      context)))

(def spec-validation
  (if goog.DEBUG
    (->interceptor
        :id :spec-validation
        :after validate-spec)
    ->interceptor))

(def standard-interceptors  [;; (when ^boolean goog.DEBUG debug)
                             spec-validation])

(defn initialize-db [_ _]
  default-app-db)

(defn set-theme [db [_ theme]]
  (->> db
       (setval [:settings :theme] theme)))

(defn set-version [db [_ version]]
  (->> db
       (setval [:version] version)))

(defn signup [cofx [_ email-pass-map]]
  (println "signup handler")
  {:db              (:db cofx)
   :firebase-signup email-pass-map})

(defn initialize-firebase [cofx [_ config]]
  {:db            (:db cofx)
   :firebase-init config})

(defn navigate [cofx [_ screen]]
  {:db       (assoc (:db cofx) :last-screen screen)
   :navigate screen})

(defn login [cofx [_ email-pass]]
  {:db             (:db cofx)
   :firebase-login email-pass})

(defn login-success [cofx [_ user]]
  {:db       (assoc (:db cofx) :user user)
   :navigate (if-some [last-screen (-> cofx :db :last-screen)]
               last-screen
               :capture)})

(defn signup-success [cofx [_ user]]
  {:db       (:db cofx)
   :navigate :login})

(defn load-user [cofx [_ _]]
  {:db                 (:db cofx)
   :firebase-load-user true})

(defn load-user-success [cofx [_ user]]
  {:db       (assoc (:db cofx) :user user)
   :navigate (if-some [last-screen (-> cofx :db :last-screen)]
               (if (= last-screen :login)
                 :capture
                 last-screen)
               :capture)})

(defn logout [cofx [_ _]]
  {:db              (dissoc (:db cofx) :user)
   :firebase-logout true})

(reg-event-db :initialize-db [standard-interceptors] initialize-db)
(reg-event-db :set-theme [standard-interceptors] set-theme)
(reg-event-db :set-version [standard-interceptors] set-version)
(reg-event-fx :signup [standard-interceptors] signup)
(reg-event-fx :initialize-firebase [standard-interceptors] initialize-firebase)
(reg-event-fx :navigate [standard-interceptors] navigate)
(reg-event-fx :login [standard-interceptors] login)
(reg-event-fx :login-success [standard-interceptors] login-success)
(reg-event-fx :signup-success [standard-interceptors] signup-success)
(reg-event-fx :load-user [standard-interceptors] load-user)
(reg-event-fx :load-user-success [standard-interceptors] load-user-success)
(reg-event-fx :logout [standard-interceptors] logout)
