(ns org.akvo.flow-api.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [duct.util.system :refer [load-system]]
            [environ.core :refer [env]]
            [clojure.tools.nrepl.server :as repl]
            [org.akvo.flow-api.utils :as utils]))

(defn secret-value
  "Reads the value of a secret from a file. It expects an
  environment variable SECRETS_MOUNT_PATH pointing to a folder
  containing secret files"
  [secret-key]
  (let [mount-path (utils/ensure-trailing-slash (:secrets-mount-path env))]
    (-> (format "%s%s" mount-path secret-key)
        slurp
        str/trim)))

(defn -main [& args]
  (let [bindings {'http-port (Integer/parseInt (:http-port env "3000"))
                  'github-auth-token (secret-value "github-auth-token")
                  'api-root (utils/ensure-trailing-slash (:api-root env))
                  'sentry-dsn (secret-value "sentry-dsn")
                  'tmp-dir (utils/ensure-trailing-slash (System/getProperty "java.io.tmpdir"))}
        system   (->> (load-system [(io/resource "org/akvo/flow_api/system.edn")] bindings)
                      (component/start))]
    (add-shutdown-hook ::stop-system #(component/stop system))
    (println "Started HTTP server on port" (-> system :http :port))
    (let [repl-server (repl/start-server :port (Integer/parseInt (:repl-port env "0")))]
      (println "REPL started on port" (:port repl-server)))))
