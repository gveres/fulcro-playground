(ns app.site.ssl-context
  (:import [io.netty.handler.ssl SslContextBuilder])
  (:require [integrant.core :as ig]
            [clojure.java.io :refer [file]]))

(defn build-ssl-context
  [cert key]
  (-> (SslContextBuilder/forServer (file cert) (file key))
      .build))

(defmethod ig/init-key
  ::ssl-context
  [_ {:keys [cert-file key-file]}]
  (build-ssl-context cert-file key-file))
