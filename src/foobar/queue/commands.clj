(ns foobar.queue.commands
  (:require [integrant.core :as ig]))

(defprotocol Boundary
  (enqueue [this message]))

(defrecord Commander [service topic]
  Boundary
  (enqueue [this message]))

(defmethod ig/init-key :foobar.queue/commands [_ options]
  (fn [config]
    (map->Commander options)))
