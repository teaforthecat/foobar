(ns duct.db.spectomic
  (:require [integrant.core :as ig]
            [provisdom.spectomic.core :as spectomic]))

;; this doesn't run or get required unless you call it on the command line like
;; lein run :duct.db/spectomic

(defmethod ig/init-key :duct.db/spectomic [_ options]
  (let [out-file (get options :out-file)
        specs (or (:spec-schema options)
                  @(resolve (:spec-var options)))
        schema (spectomic/datomic-schema specs)]
    (spit out-file schema)
    (println "wrote " out-file)
    schema))
