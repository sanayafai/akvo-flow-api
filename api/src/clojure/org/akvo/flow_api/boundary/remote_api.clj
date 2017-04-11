(ns org.akvo.flow-api.boundary.remote-api
  (:require [org.akvo.flow-api.boundary.akvo-flow-server-config :as afsc]
            [org.akvo.flow-api.component.remote-api])
  (:import [com.google.appengine.tools.remoteapi RemoteApiOptions]
           [org.akvo.flow_api.component.remote_api RemoteApi LocalApi]))

(defprotocol IRemoteApi
  (options [this instance-id]))

(extend-protocol IRemoteApi

  RemoteApi
  (options [this instance-id]
    (let [afsc (:akvo-flow-server-config this)
          port (afsc/port afsc instance-id)
          host (afsc/host afsc instance-id)
          iam-account (afsc/iam-account afsc instance-id)
          p12-path (afsc/p12-path afsc instance-id)
          remote-path (if-let [trace-path (afsc/trace-path afsc instance-id)]
                        (str "/traced_remote_api/" trace-path)
                        "/remote_api")
          options (-> (RemoteApiOptions.)
                      (.server host port)
                      (.remoteApiPath remote-path))]
      (.useServiceAccountCredential options
                                    iam-account
                                    p12-path)
      options))

  LocalApi
  (options [this instance-id]
    (let [options (-> (RemoteApiOptions.)
                      (.server "localhost" 8080))]
      (.useDevelopmentServerCredential options)
      options)))