(ns foobar.handler.cqrs
  (:require [compojure.core :refer :all]
            [integrant.core :as ig]
            [clojure.java.io :as io]))

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

(defmethod ig/init-key :foobar.handler/cqrs [_ options]
  (wrap-routes
   (context "/api" {{:keys [user-id]} :session}
            (POST "/command" request
                  (if (valid-command? (:params request))
                    (let [result (.enqueue (:queue options)
                                           (assoc (:params request) :user-id user-id))]
                      (if (:success result)
                        {:status 200 :body result}
                        {:status 503 :body "could not enqueue command"}))
                    {:status 400 :body "error message"}))
            (GET "/query" request
                 (if (valid-query? (:params request))
                   (let [result (.query (:data-source options)
                                        (assoc (:params request) :user-id user-id))]
                     (if (:success result)
                       {:status 200 :body result}
                       {:status 503 :body "could not retrieve data"}))
                   {:status 400 :body "error message"})))
   read-body))
