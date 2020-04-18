(ns app.handlers
  (:require
   ["firebase" :as firebase]
   ["firebase/auth"]
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
  (let [db (-> context :effects :db)
        old-db (-> context :coeffects :db)
        event (-> context :coeffects :event)]

    (if (some? (check-and-throw app-db-spec db event))
      (assoc-in context [:effects :db] old-db) ;; put the old db back as the new db when check fails
      context)))                               ;; otherwise return context unchanged

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

(reg-fx :firebase-signup
        (fn [{:keys [email password]}]
          (-> firebase
              (.auth)
              (.createUserWithEmailAndPassword email password))))

(defn signup [cofx [_ email-pass-map]]
  (merge cofx {:firebase-signup email-pass-map}))

(reg-fx :firebase-init
        (fn [config]
          (-> firebase (.initializeApp (clj->js config)))))

(defn initialize-firebase [cofx [_ config]]
  (merge cofx {:firebase-init config-js}))

(reg-event-db :initialize-db [spec-validation] initialize-db)
(reg-event-db :set-theme [spec-validation] set-theme)
(reg-event-db :set-version [spec-validation] set-version)
(reg-event-fx :signup [] signup)
(reg-event-fx :initialize-firebase [] initialize-firebase)
