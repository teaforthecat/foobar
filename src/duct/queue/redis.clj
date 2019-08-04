(ns duct.queue.redis
  (:require [integrant.core :as ig]
            [taoensso.carmine :as car :refer (wcar)]))

(defprotocol Boundary
  (publish [conn topic msg])
  (subscribe [conn topic handler]))

(def listeners (atom []))

(defrecord Conn [options]
  Boundary
  (publish [this topic msg]
    (wcar options (car/publish topic msg)))
  (subscribe [this topic handler]
    (let [listener (car/with-new-pubsub-listener options
                     {topic handler}
                     (car/subscribe topic))]
      (swap! listeners conj listener)
      (assoc this :listener listener))))

(defmethod ig/init-key :duct.queue/redis [_ options]
  ;; example options:
  ;; {:pool {} :spec {:uri \"redis://redistogo:pass@panga.redistogo.com:9475/\"}}
  (map->Conn options))


(defmethod ig/halt-key! :duct.queue/redis [_ conn]
  (map car/close-listener @listeners)
  #_(cond
    (:listener conn)
    (car/close-listener (:listener conn))))

(comment

  (def options {:pool {}
                :spec {:uri "redis://localhost:6379"}})

  (def conn (ig/init-key :duct.queue/redis options))

  ;; (publish conn "test" {:abc 123})
  (publish conn "demo" {:abc 123})

  (def subscriber
    (subscribe conn "test" println))


  (ig/halt-key! :duct.queue/redis subscriber)

  )
