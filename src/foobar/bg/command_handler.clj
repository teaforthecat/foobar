(ns foobar.bg.command-handler
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defmethod ig/init-key :foobar.bg/command-handler [_ options]
  (fn [msg]
    (let [db (d/db (:conn options))
          queue (:queue options)
          {{:keys [command args command-id user-id]} :value} msg
          zulu (:zulu args)
          tx-data [[:db/add (d/tempid :db.part/user) :foobar.handler.cqrs/zulu zulu]
                   ;tx meta data
                   ;; todo add to schema
                   ;; [:db/add (d/tempid :db.part/tx) :command-id command-id]
                   ]
          ;; try catch or maybe explode for an automatic retry on next poll?
          ;; if automatic retry a retry counter will need to be implemented
          ;; or maybe a retry queue?
          result @(d/transact (:conn options) tx-data)
          response {:command-id command-id
                    :tx-id (.tx (first (:tx-data result)))
                    :result :success}]
      (.publish (:pubsub options) user-id response))))
