(ns org.akvo.flow-api.endpoint.utils
  (:require [clojure.string :as s])
  (:import [java.net.URLEncoder]))

(defn url-encode [s]
  (java.net.URLEncoder/encode (str s) "UTF-8"))

(defn query-params-str [m]
  (->> (for [[k v] m
             :when (some? v)]
         (format "%s=%s" (url-encode k)
                 (url-encode v)))
       (s/join "&")))

(defn url-builder
  ([api-root instance path]
   (url-builder api-root instance path nil))
  ([api-root instance path query-params]
   (let [base-url (format "%sorgs/%s/" api-root instance)
         path-url (str base-url path)
         full-url (if query-params
                    (str path-url "?" (query-params-str query-params))
                    path-url)]
     full-url)))
