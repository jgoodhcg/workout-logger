(ns app.handlers
  (:require
   [re-frame.core :refer [reg-event-db ->interceptor reg-event-fx reg-fx dispatch]]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
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
  {:db       (:db cofx)
   :navigate screen})

(defn login [cofx [_ email-pass]]
  {:db             (:db cofx)
   :firebase-login email-pass})

(defn login-success [cofx [_ user]]
  {:db       (assoc (:db cofx) :user user)
   :navigate :capture})

(defn signup-success [cofx [_ user]]
  {:db       (:db cofx)
   :navigate :login})

(defn load-user [cofx [_ _]]
  {:db                 (:db cofx)
   :firebase-load-user true})

(defn load-user-success [cofx [_ user]]
  {:db       (assoc (:db cofx) :user user)
   :navigate :capture})

(reg-event-db :initialize-db [spec-validation] initialize-db)
(reg-event-db :set-theme [spec-validation] set-theme)
(reg-event-db :set-version [spec-validation] set-version)
(reg-event-fx :signup [spec-validation] signup)
(reg-event-fx :initialize-firebase [spec-validation] initialize-firebase)
(reg-event-fx :navigate [spec-validation] navigate)
(reg-event-fx :login [spec-validation] login)
(reg-event-fx :login-success [spec-validation] login-success)
(reg-event-fx :signup-success [spec-validation] signup-success)
(reg-event-fx :load-user [spec-validation] load-user)
(reg-event-fx :load-user-success [spec-validation] load-user-success)
