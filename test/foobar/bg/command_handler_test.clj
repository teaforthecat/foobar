(ns foobar.bg.command-handler-test
  (:require [foobar.bg.command-handler :as ch]
            [clojure.test :refer [deftest testing is]]
            ;; [datomic.client.api :as client]
            [datomic.api :as d]
            [integrant.core :as ig]))

(deftest connection
  (testing "it works"
    (let [conn (ig/init-key :duct.db/datomic {:db-uri "datomic:mem://foobar-test"
                                              :server-type :local})
          handler (ig/init-key :foobar.bg/command-handler {:conn conn})]
      (is (= 'wut (handler {:abc 1}))))))
