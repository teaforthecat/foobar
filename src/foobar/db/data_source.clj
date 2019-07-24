(ns foobar.db.data-source
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :foobar.db/data-source [_ options]
  (fn [conf]
    nil))
