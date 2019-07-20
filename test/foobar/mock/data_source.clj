(ns foobar.mock.data-source
  (:require [integrant.core :as ig]))

(defprotocol Boundary
  (query [this q]))

(defrecord MockDataSource []
  ;; duct.datomic/Boundary
  Boundary
  (query [this q]
    {:success true}))

(defmethod ig/init-key :foobar.mock/data-source [_ options]
  (fn [config]
    (map->MockDataSource options)))
