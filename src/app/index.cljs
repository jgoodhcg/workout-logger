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

(defn home-scene [props]
  (r/as-element
   (let [version         (<sub [:version])
         theme-selection (<sub [:theme])
         theme           (.-theme props)]
     [:> paper/Surface {:style (.-surface styles)}
      [:> rn/View

       [:> paper/Title {:style (.-title styles)}
        "Workout Logger"]

       ;; login
       [:> paper/TextInput
        {:label "email"
         :style (.-input styles)}]
       [:> paper/TextInput
        {:label             "password"
         :secure-text-entry true
         :style             (.-input styles)}]
       [:> paper/Button
        {:icon     "account"
         :mode     "contained"
         :style    (.-buttonLogin styles)
         :on-press #(-> nav/Actions (.push "test-tab-one"))}
        "Login"]

       ;; signup
       [:> paper/Button
        {:icon     "account-plus"
         :mode     "outlined"
         :style    (.-button styles)
         :on-press #(>evt [:signup {:email    "testFXIsolation@gmail.com"
                                    :password "test-password"}])}
        "Signup"]]])))

(defn stub-page [props]
  (r/as-element
   [:> paper/Surface {:style (.-surface styles)}
    [:> rn/View
     [:> paper/Title (:title (js->clj props :keywordize-keys true))]]]))

(defn root []
  (let [theme (<sub [:theme])]
    [:> paper/Provider {:theme (case theme
                                 :light paper/DefaultTheme
                                 :dark  paper/DarkTheme
                                 paper/DarkTheme)}
     [:> nav/Router
      [:> nav/Stack {:key "root"}
       ;; [:> nav/Scene {:key          "home"
       ;;                :title        "Home"
       ;;                :hide-nav-bar true
       ;;                :component    (paper/withTheme home-scene)}]
       [:> nav/Tabs {:key "tabar"
                     :hide-nav-bar true}
        [:> nav/Scene {:key          "test-tab-one"
                       :component    (paper/withTheme stub-page)
                       :hide-nav-bar true
                       :title        "Tab One"}]
        [:> nav/Scene {:key          "test-tab-two"
                       :component    (paper/withTheme stub-page)
                       :hide-nav-bar true
                       :title        "Tab Two"}]]]

      ]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root])))

(def version (-> expo-constants
                 (.-default)
                 (.-manifest)
                 (.-version)))

(defn init []
  ;; TODO move this to side effect
  (dispatch-sync [:initialize-firebase firebase-config])
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:set-version version])
  (start))

