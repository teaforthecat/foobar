(ns duct.queue.kafka
  (:require [integrant.core :as ig]
            [clojure.java.io :as io])
  (:import [org.apache.kafka.clients
            producer.KafkaProducer
            producer.ProducerRecord
            consumer.KafkaConsumer
            consumer.ConsumerRecord]))

(defprotocol Boundary
  (produce
    [conn message])
  (consume
    [conn topic handler]))


(defn consume-thread
  "Consume a Kafka topic or topics from a designated thread.
  `consumer-options` is the key value config pairs for KafkaConsumer
  `topics` can be a string for a single topic or a vector of strings for multiple topics
  `handler` will be applied to each message

  Returns a map with a function to close the consumer which will also exit the thread
  when the closing is complete
  "
  [consumer topics handler]
  (let [continue (atom true)
        poll-wait (java.time.Duration/ofMillis 1000)
        _ (.subscribe consumer topics)
        thread (Thread. (fn []
                          (loop []
                            (if @continue
                              (do
                                ; this do block is where we ensure that offsets aren't
                                ; committed until the handler handles each
                                (doseq [^ConsumerRecord record (.poll consumer poll-wait)]
                                  (handler {:topic (.topic record)
                                            :key (.key record)
                                            :value (.value record)
                                            :offset (.offset record)}))

                                (.commitSync consumer)
                                (recur))
                              (.close consumer)))))
        close-fn (fn []
                   (reset! continue false)
                   ;; wait for processing to finish before returning as a
                   ;; precaution
                   (.join thread))]
    (.start thread)
    {:close-fn close-fn}))

;; todo add admin
(defrecord Conn [^KafkaProducer producer ^KafkaConsumer consumer]
  Boundary
  (produce [this message]
    (let [{:keys [topic key value]} message
          record (ProducerRecord. topic key value)
          ;; we want to be sure the message was received, so block.
          ;; psuedo-async can be configured with "acks" 0
          ;; (this could be turned into an option though)
          result @(.send producer record)]
      {:topic (.topic result)
       :partition (.partition result)
       :offset (.offset result)
       :timestamp (.timestamp result)}))
  (consume [this topics handler]
    (assoc this :consuming-thread
           (consume-thread consumer topics handler))))

(defn props
  "clojure map to java Properties.
  Map should have strings as keys and string,int,boolean as values"
  [m]
  (let [props (java.util.Properties.)]
    (doseq [p m]
      (.put props (key p) (val p)))
    props))

(defmethod ig/init-key :duct.queue/kafka [_ options]
  (let [p-opts (merge (dissoc options :consumer :producer) (:producer options))
        c-opts (merge (dissoc options :consumer :producer) (:consumer options))
        producer (KafkaProducer. (props p-opts))
        consumer (KafkaConsumer. (props c-opts))]
    (map->Conn {:producer producer :consumer consumer})))

(defmethod ig/halt-key! :duct.queue/kafka [_ conn]
  (cond-> conn
    (:producer conn)
    (update :producer #(do (.close %) %))
    (:consuming-thread conn)
    ;; closes the consumer and sets :consuming-thread to nil
    (update :consuming-thread #((:close-fn %)))))


(comment


  (def options {"bootstrap.servers" "localhost:9092",
                                             :consumer
                                             {"group.id" "foobar",
                                              "enable.auto.commit" false,
                                              "key.deserializer"
                                              "org.apache.kafka.common.serialization.StringDeserializer",
                                              "value.deserializer"
                                              "org.apache.kafka.common.serialization.StringDeserializer"},
                                             :producer
                                             {"client.id" "foobar",
                                              ;; "enable.idempotence" true,
                                              "key.serializer" "org.apache.kafka.common.serialization.StringSerializer",
                                              "value.serializer" "org.apache.kafka.common.serialization.StringSerializer"}})


  (def conn (ig/init-key :duct.queue/kafka options))

  (produce conn {:topic "abc" :key "12345" :value "xyz"})

  (def c  (consume conn ["abc"] println))

  (ig/halt-key! :duct.queue/kafka c)


  )
