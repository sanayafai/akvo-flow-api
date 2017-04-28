(ns org.akvo.flow-api.endpoint.form-instance
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.boundary.form-instance :as form-instance]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn find-form [forms form-id]
  (some #(if (= (:id %) form-id)
           %
           nil)
        forms))

(defn cursor-url-fn [api-root instance-id survey-id form-id page-size]
  (fn [cursor]
    (format "%sorgs/%s/form-instances/%s/%s?%scursor=%s"
            api-root
            instance-id
            survey-id
            form-id
            (if page-size
              (format "pageSize=%s&" page-size)
              "")
            cursor)))

(defn add-cursor [form-instances api-root instance-id survey-id form-id page-size]
  (if (empty? (:form-instances form-instances))
    (dissoc form-instances :cursor)
    (update form-instances :cursor (cursor-url-fn api-root
                                                  instance-id
                                                  survey-id
                                                  form-id
                                                  page-size))))

(def params-spec (clojure.spec/keys :req-un [::spec/survey-id ::spec/form-id]
                                    :opt-un [::spec/cursor ::spec/page-size]))

(defn endpoint* [{:keys [remote-api api-root]}]
  (GET "/form-instances/:survey-id/:form-id" {:keys [email instance-id alias params]}
    (let [{:keys [survey-id
                  form-id
                  page-size
                  cursor]} (spec/validate-params params-spec
                                                 (rename-keys params
                                                              {:pageSize :page-size}))
          page-size (when page-size
                      (Long/parseLong page-size))
          user-id (user/id-by-email remote-api instance-id email)
          survey (survey/by-id remote-api instance-id user-id survey-id)
          form (find-form (:forms survey) form-id)]
      (-> remote-api
          (form-instance/list instance-id user-id form {:page-size page-size
                                                        :cursor cursor})
          (add-cursor api-root alias survey-id form-id page-size)
          (response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
