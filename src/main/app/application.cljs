(ns app.application
  (:require
   [com.fulcrologic.fulcro.application :as app]
   [app.ui :refer [client-did-mount]]
   [com.fulcrologic.fulcro.networking.websockets :as fws]))

(defonce app (app/fulcro-app
               {:remotes          {:remote (fws/fulcro-websocket-remote {:delay-start? true})}
                :client-did-mount client-did-mount}))
