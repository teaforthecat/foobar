(ns duct.db.datomic
  (:require [integrant.core :as ig]
            [datomic.api :as d]
            [clojure.java.io :as io]))


(defmethod ig/init-key :duct.db/datomic [_ options]
  (d/create-database (:db-uri options))
  (let [conn (d/connect (:db-uri options))]
    (doseq [s (:schema options)]
      ;; TODO combine into one transaction
      (d/transact conn (read-string (slurp (io/resource s))))
      (println "loaded schema " s))
    conn))
