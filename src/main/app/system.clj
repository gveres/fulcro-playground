(ns app.system
  (:require
   [integrant.core :as ig]
   [app.server]
   [aero.core :refer [read-config] :as aero]
   [clojure.java.io :as io]))

(defmethod aero/reader 'ig/ref [_ _ value]
  (ig/ref value))

(defonce system (atom nil))

(defn ig-start! []
  (let [config (:ig/system (read-config (io/resource "config.edn")))]
    (println (ig/load-namespaces config))
    (reset! system (ig/init config))))

(defn ig-halt! []
  (ig/halt! @system))
