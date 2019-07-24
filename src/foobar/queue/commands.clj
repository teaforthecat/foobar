(ns foobar.queue.commands
  (:require [integrant.core :as ig]))


(defrecord Commander [service topic]
  (enqueue [this message]))

(defmethod ig/init-key :foobar.queue/commands [_ options]
  (fn [config]
    (map->Commander options)))
