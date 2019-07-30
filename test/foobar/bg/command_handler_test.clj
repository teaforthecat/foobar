(ns foobar.bg.command-handler-test
  (:require [foobar.bg.command-handler :as ch]
            [clojure.test :refer [deftest testing is]]
            [datomic.api :as d]
            [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as g]
            [duct.queue.redis :as r]
            [duct.db.datomic]
            [foobar.handler.cqrs]
            ))


(defrecord FakeRedis []
  r/Boundary
  (publish [conn topic message]
    message)
  (subscribe [this topic handler]))

(deftest connection
  (testing "it works"
    (let [conn (ig/init-key :duct.db/datomic {:db-uri "datomic:mem://foobar-test"
                                              :schema ["foobar/db/spec-schema.edn"]})
          handler (ig/init-key :foobar.bg/command-handler {:conn conn
                                                           :pubsub (->FakeRedis)})
          example (g/generate (s/gen :foobar.handler.cqrs/example))
          result (handler {:value {:args example}})]
      ;; this means the insert worked
      (is (= (number? (:tx-id result)))))))
