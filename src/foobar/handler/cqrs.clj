(ns foobar.handler.cqrs
  (:require [compojure.api.sweet :refer [api context resource GET POST]]
            [clojure.spec.alpha :as s]
            [integrant.core :as ig]
            [clojure.java.io :as io]))

;; ::command gets re-mapped to what is passed as options(aka: config.edn)
(s/def ::command keyword?)
(s/def ::args map?)
(s/def ::command-id uuid?)
(s/def ::command-body (s/keys :req-un [::command ::args] :opt-un [::command-id]))

(s/def ::zulu number?)
(s/def ::example (s/keys :req-un [::zulu]))

(s/def ::name string?)
(s/def ::message string?)

(defn valid-command? [body]
  (let [{:keys [command args command-id]} body]
    (and command args command-id)))

(defn valid-query? [query]
  (contains? query :type))

(defn read-body
  "it would be cool if this was provided like it is in compojure.sweet.api"
  [handler]
  (fn [req]
    ;; I think this body slurp is needed because ring-defaults only provides for urlencoded form posts, not the api body that we are looking for. compojure.sweet.api would do the content negotiation and read the body into the params appropriately.
    (if (= java.io.ByteArrayInputStream (type (:body req)))
      (let [body-params (read-string (slurp (:body req)))]
        (handler (update req :params merge body-params)))
      (handler req))))

(defn command-handler [req]
  {:status 200 :body (:body-params req)})

(defmethod ig/prep-key :foobar.handler/cqrs [_ options]
  ;; re-assign
  (s/def ::command #(some (set (:commands options)) #{%}))

  options
  )


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
                                {:summary " post commands to be enqueued and processed asynchronously"
                                 :parameters {:body-params ::command-body}
                                 :consumes ["application/json" "application/edn"]
                                 :produces ["application/json" "application/edn"]
                                 :handler command-handler}}))))
  )
