(ns duct.queue.redis
  (:require [integrant.core :as ig]
            [taoensso.carmine :as car :refer (wcar)]))

(defprotocol Boundary
  (publish [conn topic msg])
  (subscribe [conn topic handler]))

(defrecord Conn [options]
  Boundary
  (publish [this topic msg]
    (wcar options (car/publish topic msg)))
  (subscribe [this topic handler]
    (assoc this :listener
           (car/with-new-pubsub-listener options
             {topic handler}
             (car/subscribe topic)))))

(defmethod ig/init-key :duct.queue/redis [_ options]
  ;; example options:
  ;; {:pool {} :spec {:uri \"redis://redistogo:pass@panga.redistogo.com:9475/\"}}
  (map->Conn options))


(defmethod ig/halt-key! :duct.queue/redis [_ conn]
  (cond
    (:listener conn)
    (car/close-listener (:listener conn))))
