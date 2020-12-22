(ns esme.callapp
  (:use [clojure.tools.logging])
  (:require [clojure.data.json :as json])
  (:require [org.httpkit.client :as http]
            [clojure.string :as str])
  (:import (java.net ConnectException SocketTimeoutException)
           (org.json.simple.parser JSONParser)
           (java.util Base64)))


(defn encode [to-encode]
  (.encodeToString (Base64/getEncoder) (.getBytes to-encode)))

(defn callAPP
  [msisdn  input sessionId as_request_type as_url
   as_connect_timeout as_read_timeout msg_as_timeout type]

  (if (every? empty? [msisdn input sessionId as_request_type])
    (let [resp (json/write-str {:message msg_as_timeout, :action false})]
      resp)
    (let [is-new? (= type "0501000101")
          data    (if is-new?
                    (json/write-str {:rc "0" :ogn msisdn
                                     :opid "013" :ur input :config "ur" :mk true :sid sessionId})
                    (let [jmgObj (read-string (.getSessionId (str msisdn "jmgObj"))) ]
                         (json/write-str {:rc "0" :jmgObj jmgObj
                                          :ogn msisdn :opid "013" :ur input
                                          :config "ur" :mk true :sid sessionId})))
          _ (infof "callApp data msisdn=%s,data=%s"msisdn data)
          options  {:socket-timeout (Integer/parseInt as_connect_timeout)
                    :conn-timeout (Integer/parseInt as_read_timeout)
                    :timeout (Integer/parseInt as_connect_timeout)             ; ms
                    :query-params {:jParams (encode data) }}
          {:keys [status body error] :as trace}  @(http/get as_url options)]
      (try
        (if error
          (let [error-msg (condp instance? error
                            ConnectException ":timeout-on-connect"
                            SocketTimeoutException ":timeout-on-read")
                resp (json/write-str {:message msg_as_timeout, :action false})]
            (errorf "Connection exception %s|%s" error-msg trace)
            resp)

          (if (not-empty body)
            (let [jsonObj (json/read-str body :key-fn keyword)
                  {:keys [jmgObj data leaf code] } jsonObj]
              (infof "Parsed response [%s|%s]" msisdn body)
              (if (or (nil? data) (empty? data))
                (do
                  (.clearSession (str msisdn "jmgObj"))
                  (json/write-str {:message msg_as_timeout, :action false}))
                ;{"jmgObj":{"tk":"MDcwMzQ3NDA1OTk6TW9uIFNlcCAxNCAyMzo1OTo1OSBXQVQgMjA
                ;yMA==","mg":"","pmg":"","stk":""},"code":200,"data":"Select spend limit\r\n1. N500\r\n2.
                ;N1000\r\n3. N2000\r\n4. N3000\r\n5. Another Amount","leaf":false,"ur":"*123*0"}

                (let [pjmg (json/read-str jmgObj)
                      pleaf (if (string? leaf) (read-string leaf) leaf)]
                  (.sessionSave (str msisdn "jmgObj") (str pjmg))
                  (json/write-str {:message data, :action pleaf}))))

            (do
              (.clearSession (str msisdn "jmgObj"))
              (json/write-str {:message msg_as_timeout, :action false}))))
        (catch Exception e
          (do
            (error "Exception: Error calling AS:" msisdn "|" (.getMessage e) )
            (.clearSession (str msisdn "jmgObj"))
            (json/write-str {:message msg_as_timeout, :action false})))))))
