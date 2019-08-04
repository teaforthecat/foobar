(ns foobar.handler.cqrs
  (:require [compojure.api.sweet :refer [api context resource GET POST]]
            [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [integrant.core :as ig]
            [manifold.stream :as ms]
            [clojure.java.io :as io]
            [aleph.http :as http]))

(s/def ::jsonable-keyword
  (st/spec
   {:spec qualified-keyword?
    :description "a namespaced keyword"
    :json-schema/type {:type "string", :format "keyword"}
    :json-schema/example "a.b/c"
    :json-schema/description "the resolvable spec and function"
    :decode/string keyword
    :encode/string #(str (namespace %2) "/" (name %2))}))

(def commands #{})
(s/def ::command ::jsonable-keyword)
(s/def ::args map?)
(s/def ::command-id uuid?)
(s/def ::command-body (s/and (s/keys :req-un [::command ::args] :opt-un [::command-id])
                             #(contains? commands (:command %))
                             #(s/get-spec (:command %))
                             #(s/valid? (:command %) (:args %))))

;; START DOMAIN

(s/def ::zulu int?)
(s/def ::example (s/keys :req-un [::zulu]))
(defn example [{:keys [command args command-id session]} deps]
  (let [kcon (:queue deps)
        ;; hmm, lots to coordinate here
        body {:command command :args args :command-id command-id :user-id (:user-id session)}
        kkey (str (or command-id (str (java.util.UUID/randomUUID))))
        msg {:topic "commands" :key kkey :value (pr-str body)}
        result (.produce kcon msg)]
    (if (:offset result)
      {:status 200 :body (assoc result :key kkey)}
      {:status 503 :body "unable to enqueue command"})))

(defn sse-handler [request-data deps]
  (let [redis (:pubsub deps)
        user-id (get-in request-data [:session :user-id] "demo")
        stream (ms/stream)]
    (.subscribe redis user-id (fn [m] (ms/put! stream (str "data: " m "\n\n"))))
    ;; aleph will handle the sse connection
    {:status 200
     :headers {"Content-Type" "text/event-stream"}
     :body stream}))

(defn websocket-handler [request-data deps]
  (if-let [socket (try
                    @(http/websocket-connection (:request deps))
                    (catch Exception e
                      nil))]
    (let [redis (:pubsub deps)
          user-id (get-in request-data [:session :user-id] "demo")
          stream (ms/stream)]
      (.subscribe redis user-id (fn [m] (ms/put! stream (str "data: " m "\n\n"))))
      (ms/connect stream socket)
      ;; only one direction for now(?)
      ;; Compojure expects some sort of HTTP response, so just give it `nil`
      nil)
    {:status 400
     :headers {"Content-Type" "application-text"}
     :body "expected a websocket request"}))

;; END DOMAIN

;; just in case?
;; (extend-protocol Renderable
;;   manifold.deferred.IDeferred
;;   (render [d _] d))

(defn command-handler
  "uses a closure around options to access the registered command functions, and calls the requested command with args deps and the optional command-id"
  [options]
  (fn [request]
    ;; request-data would include :command, :args, :command-id, and :session (which is whatever)
    (let [request-data (assoc (:body-params request) :session (:session request))
          command (:command request-data)
          deps (assoc (:deps options) :request request)]

      ;; call the registered function
      ;; we have confirmed the presence in ::command-body
      ((get-in options [:command-fns command]) request-data deps))))

(defn event-stream-handler [options]
  (fn [request]
    (let [request-data (assoc (:query-params request) :session (:session request))
          deps (assoc (:deps options) :request request)]
      ((:sse-fn options) request-data deps))))

(defn ws-handler [options]
  (fn [request]
    (let [request-data (assoc (:query-params request) :session (:session request))
          deps (assoc (:deps options) :request request)]
      ((:ws-fn options) request-data deps))))

(defmethod ig/prep-key :foobar.handler/cqrs [_ options]
  ;; global redefine, not really significant, usable by ::command-body
  (def commands (set (:commands options)))

  (let [cmd-fns (into {} (map #(vec [% (resolve (symbol %))]) commands))]
    {:command-fns cmd-fns
     :sse-fn (resolve (symbol (:sse options)))
     :ws-fn (resolve (symbol (:ws options)))
     :deps (dissoc options :commands)}))

(defmethod ig/init-key :foobar.handler/cqrs [_ options]
  (api
   {:swagger
    {:ui "/api-docs"
     :spec "/swagger.json"
     :data {:info {:title "Sample API"
                   :description "Compojure Api example"}
            :tags [{:name "api", :description "some apis"}]
            :consumes ["application/json"]
            :produces ["application/json"]}}}
   (context "/api" {{:keys [user-id]} :session}
            :coercion :spec
            (context "/command" []
                     (resource {:post
                                {:summary " post commands to a handler which is resolved by using the :command given"
                                 :parameters {:body-params ::command-body}
                                 :consumes ["application/json" "application/edn"]
                                 :produces ["application/json" "application/edn"]
                                 :handler (command-handler options)}}))
            (context "/events" []
                     (resource {:get
                                {:consumes ["text/event-stream"]
                                 :produces ["text/event-stream"]
                                 :handler (event-stream-handler options)}}))
            (context "/ws" []
              (resource {:get
                         {:handler (ws-handler options)}})))))
