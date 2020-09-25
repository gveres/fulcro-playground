(ns app.parser
  (:require
   [app.resolvers]
   [app.mutations]
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.viz.ws-connector.core :as p.connector]
   [com.wsscode.pathom.connect :as pc]
   [taoensso.timbre :as log]))

(def resolvers [app.resolvers/resolvers app.mutations/mutations])

(def CONNECT_PARSER? true)

(def pathom-parser
  (cond->> (p/parser {::p/env     {::p/reader                 [p/map-reader
                                                               pc/reader2
                                                               pc/ident-reader
                                                               pc/index-reader]
                                   ::pc/mutation-join-globals [:tempids]}
                      ::p/mutate  pc/mutate
                      ::p/plugins [(pc/connect-plugin {::pc/register resolvers})
                                   p/error-handler-plugin]})
    CONNECT_PARSER?
    (p.connector/connect-parser
                                        ; parser-id is optional, but highly encouraged, without this
                                        ; the application can't know about the parser identity and will not
                                        ; be able to remember data about query history across parser connections
      {::p.connector/parser-id ::app-parser})))

(defn api-parser [env query]
  (log/info "Process" query)
  (pathom-parser env query))
