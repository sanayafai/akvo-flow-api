(ns org.akvo.flow-api.boundary.survey
  (:refer-clojure :exclude [list])
  (:require [clojure.core.cache :as cache]
            org.akvo.flow-api.component.cache
            org.akvo.flow-api.component.remote-api
            [org.akvo.flow-api.datastore :as ds]
            [org.akvo.flow-api.datastore.survey :as survey]))

(defn get-survey-definition [{:keys [cache]} instance-id user-id survey-id]
  (cache/lookup @cache [:survey-definitions instance-id user-id survey-id]))

(defn put-survey-definition [{:keys [cache]} instance-id user-id survey-id survey-definition]
  (swap! cache cache/miss [:survey-definitions instance-id user-id survey-id] survey-definition))

(defn list [this instance-id user-id folder-id]
  (ds/with-remote-api this instance-id
    (doall (survey/list user-id folder-id))))

(defn by-id [{:keys [survey-cache] :as this} instance-id user-id survey-id]
  (if-let [survey-definition (get-survey-definition survey-cache
                               instance-id
                               user-id
                               survey-id)]
    survey-definition
    (ds/with-remote-api this instance-id
      (let [survey-definition (survey/by-id user-id survey-id)]
        (put-survey-definition survey-cache
          instance-id
          user-id
          survey-id
          survey-definition)
        survey-definition))))
