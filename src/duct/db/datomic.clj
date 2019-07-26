(ns duct.db.datomic
  (:require [integrant.core :as ig]
            [datomic.api :as d]))

(defmethod ig/init-key :duct.db/datomic [_ options]
  (d/create-database (:db-uri options))
  (d/connect (:db-uri options)))



(defmethod ig/halt-key! :duct.db/datomic [_ conn]
  (.close conn))
