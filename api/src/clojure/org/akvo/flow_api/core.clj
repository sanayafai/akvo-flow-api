(ns org.akvo.flow-api.core
  (:require [clojure.java.io :as io]
            [org.akvo.flow-api.akvo-flow-server-config :as config]
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.folder :as folder]
            [org.akvo.flow-api.datastore.form-instance :as form-instance]
            [org.akvo.flow-api.datastore.survey :as survey])
  (:import [java.util.logging LogManager]))

;; Configure Datanucleus logging
(.readConfiguration (LogManager/getLogManager)
                    (io/input-stream (io/resource "logging.properties")))

(comment

  (defn remote-api-spec [instance-map instance-id]
    (let [auth-token (System/getenv "GITHUB_API_KEY")
          host (str instance-id ".appspot.com")
          iam-account (get-in instance-map [instance-id "serviceAccountId"])
          p12-file (config/get-p12 instance-id auth-token)]
      {:host host
       :iam-account iam-account
       :p12-path (.getAbsolutePath p12-file)}))

  (let [auth-token (System/getenv "GITHUB_API_KEY")
        email "@gmail.com"
        instance-map (config/get-instance-map auth-token)
        alias-map (config/get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (config/get-p12 instance-id auth-token)]
    (folder/get-filtered-folders {:host host
                                  :iam-account iam-account
                                  :p12-path (.getAbsolutePath p12-file)}
                                 email
                                 "27009117"))

  (let [auth-token (System/getenv "GITHUB_API_KEY")
        email "@gmail.com"
        instance-map (config/get-instance-map auth-token)
        alias-map (config/get-alias-map instance-map)
        instance-id (get alias-map "uat1")
        host (str instance-id ".appspot.com")
        port 443
        iam-account (get-in instance-map [instance-id "serviceAccountId"])
        p12-file (config/get-p12 instance-id auth-token)]
    (survey/get-filtered-surveys {:host host
                                  :iam-account iam-account
                                  :p12-path (.getAbsolutePath p12-file)}
                                 email
                                 "24109115"))

  (def auth-token (System/getenv "GITHUB_API_KEY"))
  (def instance-map (config/get-instance-map auth-token))

  (ds/with-local-api
    (folder/get-filtered-folders "@akvo.org" "0"))

  (ds/with-local-api
    (survey/get-filtered-surveys "@akvo.org" "78859123"))

  (ds/with-local-api
    (survey/get-survey-definition "@akvo.org" ""))

  (ds/with-local-api
    (let [ds (DatastoreServiceFactory/getDatastoreService)]
      (let [sd (survey/get-survey-definition "@akvo.org" "100909115")
            fd (first (:forms sd))]
        (form-instance/fetch-form-instances ds fd {:page-size 5}))))

  (import com.google.appengine.api.datastore.Entity)
  (import com.google.appengine.api.datastore.Query)
  (import com.google.appengine.api.datastore.DatastoreServiceFactory)
  (import com.google.appengine.api.datastore.FetchOptions$Builder)

  (ds/with-local-api
    (let [e (doto (Entity. "Survey")
              (.setProperty "createdAt" (java.util.Date.))
              (.setProperty "name" (str "Test " (System/nanoTime))))
          ds (DatastoreServiceFactory/getDatastoreService)]
      (.put ds e)))

  (ds/with-local-api
    (let [ds (DatastoreServiceFactory/getDatastoreService)
          q (Query. "Question")]
      (.asList (.prepare ds q) (FetchOptions$Builder/withDefaults)))))