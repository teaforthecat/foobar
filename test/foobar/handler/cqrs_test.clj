(ns foobar.handler.cqrs-test
  (:require [clojure.test :refer [deftest testing is]]
            [foobar.handler.cqrs :as cqrs]
            [integrant.core :as ig]
            [foobar.mock.queue :refer [map->MockQueue]]
            [foobar.mock.data-source :refer [map->MockDataSource]]
            [ring.mock.request :as mock]))

(def mock-queue (map->MockQueue {}))
(def mock-data-source (map->MockDataSource {}))

(deftest api-requests
  (testing "invalid command results in 400"
    (let [handler (ig/init-key :foobar.handler/cqrs {:queue nil :data-source nil})
          response (handler (mock/request :post "/api/command" (pr-str {:wut true})))]
      (is (= (:status response) 400))))
  (testing "valid command results in 200"
    (let [handler (ig/init-key :foobar.handler/cqrs {:queue mock-queue :data-source nil})
          response (handler (mock/request :post "/api/command" (pr-str {:command :wut
                                                                        :args {:x 1}
                                                                        :command-id 123})))]
      (is (= (:status response) 200))))
  (testing "invalid query results in 400"
    (let [handler (ig/init-key :foobar.handler/cqrs {:queue nil :data-source nil})
          response (handler (mock/request :get "/api/query" {:wut true}))]
      (is (= (:status response) 400))))
  (testing "valid query results in 200"
    (let [handler (ig/init-key :foobar.handler/cqrs {:queue nil :data-source mock-data-source})
          response (handler (assoc (mock/request :get "/api/query")
                                   ;; middleware is not loaded here so we bypass
                                   ;; the querystring rendering and parsing here
                                   :params {:type :graphql
                                            :q {:x 1}}))]
      (is (= (:status response) 200)))))

(deftest sse
  (testing "a stream of numbers"
    (let [redis (ig/init-key :duct.queue/redis {:spec {:uri "redis://localhost:6379"}})
          handler (ig/init-key :foobar.handler/cqrs {:pubsub redis
                                                     :sse-fn :foobar.handler.cqrs/sse-handler})
          _ (.publish redis "demo" "hi")
          response (handler (-> (mock/request :get "/api/events")
                                (mock/content-type "text/event-stream")))]
      (.publish redis "demo" "hi")
      (is (= 'wut response #_(-> response :body)))
      )))
