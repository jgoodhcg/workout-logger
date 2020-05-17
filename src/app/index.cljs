(ns app.index
  (:require
    ["expo" :as ex]
    ["expo-constants" :as expo-constants]
    ["firebase" :as firebase]
    ["react-native" :as rn]
    ["react" :as react]
    ["react-native-router-flux" :as nav]
    ["react-native-paper" :as paper]
    [camel-snake-kebab.core :as csk]
    [camel-snake-kebab.extras :as cske]
    [reagent.core :as r]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [shadow.expo :as expo]
    [applied-science.js-interop :as j]
    [app.fx]
    [app.handlers]
    [app.subscriptions]
    [app.secrets :as secrets]
    [app.helpers :refer [<sub >evt]]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def firebase-config {:apiKey            secrets/firebase-api-key
                      :authDomain        "workout-logger-c7ccd.firebaseapp.com"
                      :databaseURL       "https://workout-logger-c7ccd.firebaseio.com"
                      :projectId         "workout-logger-c7ccd"
                      :storageBucket     "workout-logger-c7ccd.appspot.com"
                      :messagingSenderId "560616186938"
                      :appId             "1:560616186938:web:57ff1a8dcc70d738fd0103"})

(def styles
  ^js (-> {:surface
           {:flex            1
            :justify-content "center"}

           :title
           {:align-self "center"}

           :button
           {:margin 8}

           :buttonLogin
           {:margin        8
            :margin-bottom 128}

           :input
           {:margin 8}

           }
          (#(cske/transform-keys csk/->camelCase %))
          (clj->js)
          (rn/StyleSheet.create)))

(def login-form (r/atom {:email    ""
                         :password ""}))

(defn login-screen [props]
  (r/as-element

   (let [version         (<sub [:version])
         theme-selection (<sub [:theme])
         theme           (.-theme props)
         email           (:email @login-form)
         password        (:password @login-form)]

     [:> paper/Surface {:style (.-surface styles)}
      [:> rn/View

       [:> paper/Title {:style (.-title styles)}
        "Workout Logger"]

       ;; login
       [:> paper/TextInput
        {:label          "email"
         :style          (.-input styles)
         :default-value  email
         :on-change-text (fn [text]
                           (swap! login-form #(assoc % :email text)))}]
       [:> paper/TextInput
        {:label             "password"
         :secure-text-entry true
         :style             (.-input styles)
         :default-value     password
         :on-change-text    (fn [text]
                              (swap! login-form #(assoc % :password text)))}]
       [:> paper/Button
        {:icon     "account"
         :mode     "contained"
         :style    (.-buttonLogin styles)
         :on-press #(>evt [:login @login-form])}
        "Login"]

       ;; signup
       [:> paper/Button
        {:icon     "account-plus"
         :mode     "outlined"
         :style    (.-button styles)
         :on-press #(>evt [:navigate :signup])}
        "Signup"]]])))

(def signup-form (r/atom {:email    ""
                          :password ""}))

(defn signup-screen [props]
  (r/as-element

   (let [version         (<sub [:version])
         theme-selection (<sub [:theme])
         theme           (.-theme props)
         email           (:email @signup-form)
         password        (:password @signup-form)
         repeat-password (:repeat-password @signup-form)]

     [:> paper/Surface {:style (.-surface styles)}
      [:> rn/View

       [:> paper/Title {:style (.-title styles)}
        "Workout Logger"]

       [:> paper/TextInput
        {:label          "email"
         :style          (.-input styles)
         :default-value  email
         :on-change-text (fn [text]
                           (swap! signup-form #(assoc % :email text)))}]
       [:> paper/TextInput
        {:label             "password"
         :secure-text-entry true
         :style             (.-input styles)
         :default-value     password
         :on-change-text    (fn [text]
                              (swap! signup-form #(assoc % :password text)))}]

       [:> paper/Button
        {:icon     "account-plus"
         :mode     "contained"
         :style    (.-buttonLogin styles)
         :on-press #(>evt [:signup @signup-form])}
        "Signup"]

       [:> paper/Button
        {:icon     "account"
         :mode     "outlined"
         :style    (.-button styles)
         :on-press #(>evt [:navigate :login])}
        "Login"]]])))

(defn stub-screen [props]
  (r/as-element
   (let [email [<sub [:email]]
         title (:title (js->clj props :keywordize-keys true))]
     [:> paper/Surface {:style (.-surface styles)}
      [:> rn/View
       [:> paper/Text email]
       [:> paper/Title title]
       [:> paper/Button {:icon "logout"
                         :on-press #(>evt [:logout])}
        "Signout"]]])))

(defn root []
  (let [theme (<sub [:theme])]
    [:> paper/Provider {:theme (case theme
                                 :light paper/DefaultTheme
                                 :dark  paper/DarkTheme
                                 paper/DarkTheme)}
     [:> nav/Router
      [:> nav/Stack {:key "root"}

       [:> nav/Scene {:key "loading"
                      :initial true
                      :hide-nav-bar true
                      :component (paper/withTheme stub-screen) :title "loading"}]
       ;; auth
       [:> nav/Scene {:key          "login"
                      :hide-nav-bar true
                      :component    (paper/withTheme login-screen)}]
       [:> nav/Scene {:key          "signup"
                      :hide-nav-bar true
                      :component    (paper/withTheme signup-screen)}]

       ;; app
       ;; TODO use custom component
       ;; https://github.com/aksonov/react-native-router-flux/blob/master/docs/API.md#custom-tab-bar-component
       [:> nav/Tabs {:key              "tabbar"
                     ;; tab bar press override
                     ;; is only to push all navigation actions through re-frame fx
                     ;; might be useful for analytics
                     :tab-bar-on-press #(>evt [:navigate (-> %
                                                             (js->clj :keywordize-keys true)
                                                             (:navigation)
                                                             (:state)
                                                             (:key)
                                                             (keyword))])
                     :hide-nav-bar     true}
        [:> nav/Scene {:key          "capture"
                       :hide-nav-bar true
                       :component    (paper/withTheme stub-screen) :title "Capture"}]
        [:> nav/Scene {:key          "list"
                       :hide-nav-bar true
                       :component    (paper/withTheme stub-screen) :title "List"}]
        [:> nav/Scene {:key          "data"
                       :hide-nav-bar true
                       :component    (paper/withTheme stub-screen) :title "data"}]
        [:> nav/Scene {:key          "profile"
                       :hide-nav-bar true
                       :component    (paper/withTheme stub-screen) :title "profile"}]]]

      ]]))

(defn start
  {:dev/after-load true}
  []
  (dispatch-sync [:load-user])
  (expo/render-root (r/as-element [root])))

(def version (-> expo-constants
                 (.-default)
                 (.-manifest)
                 (.-version)))

(defn init []
  ;; TODO move this to side effect
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:initialize-firebase firebase-config])
  (dispatch-sync [:set-version version])
  (dispatch-sync [:load-user])
  (start))

