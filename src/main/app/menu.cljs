(ns app.menu
  (:require [com.fulcrologic.fulcro.components :refer [defsc] :as comp]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu :refer [ui-menu]]
            [app.auth0]
            [com.fulcrologic.semantic-ui.collections.menu.ui-menu-item :refer [ui-menu-item]]))


(defsc MainMenu [this props]
  {}
  (ui-menu
    {:vertical false :fluid true
     :children [(ui-menu-item {:key "home" :onClick #(dr/change-route this ["main"])} "Home" )
                (ui-menu-item {:key "enemies" :onClick #(dr/change-route this ["personlist" "enemies"])} "Enemies" )
                (ui-menu-item {:key "population" :onClick #(dr/change-route this ["population"])} "Population" )
                (ui-menu-item {:key "friends" :onClick #(dr/change-route this ["personlist" "friends"])} "Friends" )
                (ui-menu-item {:key "logout" :onClick #(.logout @app.auth0/webautha)} "Log out")]}
    )
  )

(def ui-mainmenu (comp/factory MainMenu))

