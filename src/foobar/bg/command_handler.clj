(ns foobar.bg.command-handler
  (:require [integrant.core :as ig]
            [datomic.api :as da]))

(defmethod ig/init-key :foobar.bg/command-handler [_ options]
  (fn [msg]
    (let [db (da/db (:conn options))]
      (println db))))
