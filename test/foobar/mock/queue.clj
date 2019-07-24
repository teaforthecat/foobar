(ns foobar.mock.queue
  (:require [integrant.core :as ig]
            [duct.queue.kafka :as k]))


(defrecord MockQueue []
  k/Boundary
  (produce [this message]
    {:success true})
  (consume [this topic handler]
    :ok))

(defmethod ig/init-key :foobar.mock/queue [_ options]
  (fn [config]
    (map->MockQueue options)))
