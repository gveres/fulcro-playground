(ns app.client
  (:import goog.net.Cookies)
  (:require
   [app.application :refer [app]]
   [app.ui :as ui]
   [app.auth0 :refer [webauthp webautha]]
   [com.fulcrologic.fulcro.application :as app]
   [cljs.core.async :refer [go]]
   [cljs.core.async.interop :refer-macros [<p!]]))

(def cookies (Cookies. js/document))

(defn init-app [webauth]
  (go
    (let
        [api-token (<p! (.getTokenSilently webauth #js {:audience         "slow-sheets-api"
                                                        :useRefreshTokens true
                                                        :cacheLocation    "localstorage"}))]
      (js/console.dir api-token)
      (.set cookies "sente-hs-token" api-token #js{:secure true :sameSite "lax"})
      (app/mount! app ui/Root "app"))))

(defn ^:export init []
  (go
    (try
      (js/console.log "Checking for auth")
      (let [webauth   (<p! webauthp)
            logged-in (<p! (.isAuthenticated webauth))]
        (reset! webautha webauth)

        (if logged-in
          (init-app webauth)

          (let [path (.-search js/window.location)]
            (if (re-find #".*code=.*state=.*" path)
              (do (.handleRedirectCallback webauth)
                  (.replaceState js/window.history {} (.-title js/document) "/")
                  (init-app webauth))
              (app/mount! app ui/Landing "app")))))
      (catch js/Error err
        ;; (.logout webauth)
        (js/alert err)
        (js/console.dir err)
        ))
    )
  (js/console.log "Loaded"))

(defn ^:export refresh []
  ;; re-mounting will cause forced UI refresh
  ;; (app/mount! app ui/Root "app")
  (js/console.log "Hot reload"))
