(ns foobar.mock.queue
  (:require [integrant.core :as ig]))

(defprotocol Boundary
  (enqueue [this message]))

(defrecord MockQueue []
  ;; duct.kafka/Boundary
  Boundary
  (enqueue [this message]
    {:success true}))

(defmethod ig/init-key :foobar.mock/queue [_ options]
  (fn [config]
    (map->MockQueue options)))
