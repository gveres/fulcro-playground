(ns app.site.ws-auth
  "The token based authentication and authorization backends."
  (:require [buddy.auth.protocols :as proto]
            [buddy.auth.middleware :refer [wrap-authentication]]
            [buddy.auth.accessrules :refer [error restrict]]
            [buddy.auth :refer [authenticated?]]
            [buddy.sign.jwt :as jwt]
            [aleph.middleware.cookies :refer [wrap-cookies]]
            [ring.util.response :refer [status response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]))

(defn- handle-unauthorized-default
  "A default response constructor for an unathorized request."
  [request]
  (if (authenticated? request)
    {:status 403 :headers {} :body "Permission denied"}
    {:status 401 :headers {} :body "Unauthorized"}))

(defn- parse-cookie-token-header
  [request token-name]
  (get-in request [:cookies token-name :value]))

(defn jws-in-cookies-backend
  "Create an instance of the jws (json web signature)
  based authentication backend.
  This backends also implements authorization workflow
  with some defaults. This means that you can provide
  own unauthorized-handler hook if the default not
  satisfies you."
  [{:keys [secret unauthorized-handler options token-name on-error]
    :or   {token-name "token"}}]
  (reify
    proto/IAuthentication
    (-parse [_ request]
      (let [token (parse-cookie-token-header request token-name)]
        token))

    (-authenticate [_ request data]
      (try
        (jwt/unsign data secret options)
        (catch clojure.lang.ExceptionInfo e
          (let [_ (ex-data e)]
            (print e)
            (when (fn? on-error)
              (on-error request e))
            nil))))

    proto/IAuthorization
    (-handle-unauthorized [_ request metadata]
      (if unauthorized-handler
        (unauthorized-handler request metadata)
        (handle-unauthorized-default request)))))

(defn authenticated-user?
  [request]
  (if (:identity request)
    true
    (error)))

(defn wrap-with-mw [handler secret]
  (let [backend (jws-in-cookies-backend {:secret     secret
                                         :token-name "sente-hs-token"})]
    (-> handler
        (restrict {:handler  authenticated-user?
                   :on-error (fn [_ _] (status (response "Nope") 403))})
        (wrap-authentication backend)
        wrap-cookies
        wrap-keyword-params
        wrap-params
        )))

