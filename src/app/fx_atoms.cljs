(ns app.fx-atoms)

;; These are in their own name space so they don't get reset on hot reload.
;; Editing this page will likely throw a Type error for subscribing the `onAuthStateChanged` callback.
;; Dismissing or restarting the application will resolve the issue.

(def auth (atom nil))

(def auth-state-subscription (atom nil))
