(ns foobar.bg.processor
  (:require [integrant.core :as ig]))



(defmethod ig/init-key :foobar.bg/processor [_ options]
  (let [{:keys [queue topics handler]} options]
    (assoc options
           :queue
           ;; start the thread
           (.consume queue topics handler))))

;; consumer will close when :duct.queue/kafka is ig/halt-key!'d
