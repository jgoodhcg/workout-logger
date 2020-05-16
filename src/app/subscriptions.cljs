(ns app.subscriptions
  (:require [re-frame.core :refer [reg-sub subscribe]]
            [com.rpl.specter :as sp :refer [select
                                            select-one
                                            select-one!]]))

(defn version [db _]
  (->> db
       (select-one! [:version])))

(defn theme [db _]
  (->> db
       (select-one! [:settings :theme])))

(defn get-user [db _]
  (->> db :user))

(defn get-email [user _]
  (if-some [email (->> user :email)] email "user not logged in"))

(reg-sub :version version)
(reg-sub :theme theme)
(reg-sub :user get-user)
(reg-sub :email
         (fn [_ _] (subscribe [:user]))
         get-email)
