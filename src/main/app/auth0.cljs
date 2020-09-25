(ns app.auth0
  (:require
   ["@auth0/auth0-spa-js" :refer (createAuth0Client)]))


(def webautha (atom nil))

(def webauthp
  "The auth0 instance creation promise used to login and make requests to Auth0"
  (createAuth0Client (clj->js {:allowSignUp        false
                               :domain             "YOUR_AUTH0_DOMAIN"
                               :client_id          "YOUR_AUTH0_CLIENT_ID" 
                               :rememberLastLogin  true
                               :responseType       "token id_token"
                               :scope              "openid profile email"
                               :useRefreshTokens   true
                               :cacheLocation      "localstorage"
                               :audience           "YOUR_AUTH0_API_AUDIENCE"
                               :redirect_uri       "YOUR_AUTH0_REDIRECT_URI"
                               :languageDictionary {:title "Log in to My Nice App"}})))
