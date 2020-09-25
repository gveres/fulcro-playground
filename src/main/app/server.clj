(ns app.server
  (:require
   [app.parser :refer [api-parser]]
   [app.site.ws-auth :refer [wrap-with-mw]]
   [aleph.http :as http]
   [bidi.ring :refer [make-handler]]
   [ring.util.response :as res]
   [integrant.core :as ig]
   [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]]
   [com.fulcrologic.fulcro.networking.websockets :as fws]
   [aleph.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]))

(def ^:private not-found-handler
  (fn [_]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "Not Found"}))


(def websockets
  (fws/start! (fws/make-websockets
                api-parser
                {:http-server-adapter (get-sch-adapter)
                 :parser-accepts-env? true
                 ;; See Sente for CSRF instructions
                 :sente-options       {:csrf-token-fn nil
                                       :user-id-fn    #(get-in % [:identity :sub])}})))

(defn ws-handler [token-secret]
  (-> not-found-handler
      (fws/wrap-api websockets)
      (wrap-with-mw token-secret)
      wrap-content-type))

(def static-handler
(-> not-found-handler
    (wrap-resource "public")))


(defn site-handler [token-secret]
  (make-handler ["/" {"chsk" (ws-handler token-secret)
                      ""     (fn [_] (res/content-type (res/resource-response "index.html" {:root "public"}) "text/html"))
                      true   static-handler}]))


(defonce server-instance (atom nil))

;; register with integrant
(defmethod ig/init-key
  ::web
  [_ {:keys [token-secret] :as config}]
  (reset! server-instance
          (http/start-server (site-handler token-secret) config)))

(defmethod ig/halt-key!
  ::web
  [_ _]
  (when @server-instance
    (.close @server-instance)
    (:stop-fn websockets)
    (reset! server-instance nil)))


(comment
  (:stop-fn! websockets))
