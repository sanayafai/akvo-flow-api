(ns org.akvo.flow-api.datastore.user-test
  (:require  [clojure.test :refer :all]
             [org.akvo.flow-api.datastore :as ds]
             [org.akvo.flow-api.datastore.user :as user]
             [org.akvo.flow-api.fixtures :as fixtures]
             [org.akvo.flow-api.boundary.user :as user-cache]
             [akvo.commons.gae :as gae]
             [akvo.commons.gae.query :as query]))

(defn uuid [] (str (java.util.UUID/randomUUID)))
(def local-ds {:hostname "localhost" :port 8888})

(def system {:components
             {:remote-api #'org.akvo.flow-api.component.remote-api/local-api
              :user-cache #'org.akvo.flow-api.component.cache/ttl-memory-cache
              :unknown-user-cache #'org.akvo.flow-api.component.cache/ttl-memory-cache}
             :dependencies {:remote-api []
                            :user-cache []}
             :endpoints {}
             :config {:user-cache {:ttl 86400000}
                      :unknown-user-cache {:ttl 600000}}})

(use-fixtures :once (fixtures/system system))

(deftest user-tests
  (ds/with-remote-api (:remote-api fixtures/*system*) "akvoflowsanbox"
    (testing "Non existing user"
      (is (nil? (user/id "no-such@user.com"))))

    (testing "Existing users"
      (are [email] (number? (user/id email))
        "akvo.flow.user.test@gmail.com"
        "akvo.flow.user.test2@gmail.com"
        "akvo.flow.user.test3@gmail.com"))))

(defn find-user [unique-email]
  (gae/with-datastore [ds unique-email]
    (first (iterator-seq (.iterator (akvo.commons.gae.query/result ds
                                      {:kind "User"
                                       :filter (query/= "emailAddress" unique-email)}))))))

(defn create-user [unique-email]
  (gae/with-datastore [ds unique-email]
    (gae/put! ds "User" {"emailAddress" unique-email}))

  (fixtures/try-for "GAE took too long to return results" 10
    (find-user unique-email)))

(defn delete-user [unique-email]
  (let [user (find-user unique-email)]
    (gae/with-datastore [ds local-ds]
      (.delete ds (into-array [(.getKey user)]))))

  (fixtures/try-for "GAE took too long to return results" 10
    (not (find-user unique-email))))

(deftest user-cache
  (let [remote-api (assoc (:remote-api fixtures/*system*)
                     :user-cache (:user-cache fixtures/*system*)
                     :unknown-user-cache (:unknown-user-cache fixtures/*system*))]
    (testing "User is cached"
      (let [unique-email (uuid)]
        (create-user unique-email)
        (is (some? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))
        (delete-user unique-email)
        (is (some? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))))

    (testing "Cache also if the user does not exist"
      (let [unique-email (uuid)]
        (is (nil? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))
        (create-user unique-email)
        (is (nil? (user-cache/id-by-email remote-api "akvoflowsanbox" unique-email)))))))