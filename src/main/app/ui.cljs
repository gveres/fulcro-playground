(ns app.ui
  (:require
   [app.mutations :as api]
   [app.auth0]
   [com.fulcrologic.fulcro.dom :as dom]
   [taoensso.timbre :as log]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [app.menu :refer [ui-mainmenu]]
   [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
   [com.fulcrologic.semantic-ui.elements.loader.ui-loader :refer [ui-loader]]
   [com.fulcrologic.semantic-ui.elements.image.ui-image :refer [ui-image]]
   [com.fulcrologic.semantic-ui.elements.container.ui-container :refer [ui-container]]
   [com.fulcrologic.semantic-ui.elements.header.ui-header :refer [ui-header]]
   [com.fulcrologic.semantic-ui.elements.list.ui-list :refer [ui-list]]
   [com.fulcrologic.semantic-ui.views.card.ui-card :refer [ui-card]]
   [com.fulcrologic.semantic-ui.views.card.ui-card-group :refer [ui-card-group]]
   [com.fulcrologic.semantic-ui.views.card.ui-card-header :refer [ui-card-header]]
   [com.fulcrologic.semantic-ui.views.card.ui-card-content :refer [ui-card-content]]
   [com.fulcrologic.semantic-ui.views.card.ui-card-description :refer [ui-card-description]]
   [com.fulcrologic.semantic-ui.elements.list.ui-list-item :refer [ui-list-item]]))

(defsc Person [this {:person/keys [name age id] :as props} {:keys [onDelete]}]
  {:query [:person/id :person/name :person/age]
   :ident :person/id}
  (ui-card {:key (str "card/" id)
            :content
            [(ui-image {:key id
                        :src (str "https://api.adorable.io/avatars/285/" name age ".png")})
             (ui-card-content {:key (str "cc/" id)}
                              [(ui-card-header {:content name :key name})
                               (ui-card-description {:key id :content (str "Age: " age)})])
             (when onDelete (ui-card-content {:extra true
                                              :key   (str "cce/" id)}
                                             (ui-button {:color   "red"
                                                         :basic   true
                                                         :content "Remove"
                                                         :onClick #(onDelete id)})))]}))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc PersonList [this {:list/keys [id label people] :as props}]
  {:query         [:list/id :list/label {:list/people (comp/get-query Person)}] ; (5)
   :ident         (fn [] [:list/id (:list/id props)])
   :route-segment ["personlist" :list/id]
   :will-enter    (fn [app {:list/keys [id] :as route-params}]
                    (log/info "Will enter personlist route with params " route-params)
                    (dr/route-deferred [:list/id (keyword id)]
                                       #(df/load app [:list/id (keyword id)] PersonList
                                                 {:post-mutation `dr/target-ready
                                                  :post-mutation-params
                                                  {:target [:list/id (keyword id)]}})))}
  (let [delete-person
        (fn [person-id] (comp/transact! this [(api/delete-person {:list/id id :person/id person-id})]))] ; (4)
    (ui-container
      {:key (str "list/" id)
       :content
       [(ui-header {:as :h3 :key "header"} label)
        (ui-card-group {:key (str "card-group/" id)
                        :content
                        [(map #(ui-person (comp/computed % {:onDelete delete-person})) people)]})]})))

(def ui-person-list (comp/factory PersonList))

(defsc Population [this {:keys [everyone] :as props}]
  {:query         [{:everyone (comp/get-query Person)}]
   :ident         (fn [_ _] [:component/id ::population])
   :route-segment ["population"]
   :will-enter    (fn [app params]
                    (dr/route-deferred [:component/id ::population]
                                       #(df/load app :everyone Population
                                                 {:post-mutation `dr/target-ready
                                                  :post-mutation-params
                                                  {:target [:component/id ::population]}})))}
  (ui-container
    {:key "everyone"
     :content
     [(ui-header {:as :h3 :key "c/everyone"} "Everyone")
      (ui-card-group {:key (str "cg/everyone")
                      :content
                      [(map ui-person everyone)]})]}))

(def ui-population (comp/factory Population))

(defsc Main [this props]
  {:ident         (fn [] [:component/id ::main])
   :query         [:main]
   :initial-state {:main "stuff"}
   :route-segment ["main"]
   :will-enter    (fn [app route-params]
                    (log/info "Will enter main" route-params)
                    (dr/route-immediate [:component/id ::main]))
   :will-leave    (fn [this props]
                    (log/info (comp/get-ident this) "props" props)
                    true)}
  (ui-container
    {}
    (ui-header {:as :h3} "Main")))

;; Routing
(defrouter TopRouter [this {:keys [current-state pending-path-segment]}]
  {:router-targets [Main PersonList Population]}
  (case current-state
    :pending (ui-loader {:active true})
    :failed  (dom/div "Loading seems to have failed. Try another route.")
    (dom/div "Unknown route")))

(def ui-top-router (comp/factory TopRouter))

(defsc Root [this {:root/keys [router] :as props}]
  {:query         [{:root/router (comp/get-query TopRouter)}]
   :initial-state {:root/router {}}}
  (ui-container
    {}
    (ui-mainmenu {})
    (dom/div
      (ui-top-router router))))

(defsc Landing [this props]
  {}
  (ui-button {:content "Log in"
              :onClick #(.loginWithRedirect @app.auth0/webautha)}))

(defn client-did-mount [app]
  (dr/change-route app ["main"]))
