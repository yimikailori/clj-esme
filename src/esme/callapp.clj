(ns esme.callapp
  (:use [clojure.tools.logging])
  (:require [clojure.data.json :as json]
            [clojure.data.codec.base64 :as b64]
            [org.httpkit.client :as http]
            [clojure.string :as str])
  (:import (java.net ConnectException SocketTimeoutException)
           (org.json.simple.parser JSONParser)))


(defn callAPP
  [msisdn  input sessionId as_request_type as_url
   as_connect_timeout as_read_timeout msg_app_error]

  (if (every? empty? [msisdn input sessionId as_request_type])
    ;{"rc":"0","ogn":"07034740599","opid":"011","ur":"*123*0","config":"ur","mk":"T","sid":"1600
    ;08504957307034740599"}

    ;{"rc":"0","jmgObj":{"tk":"MDcwMzQ3NDA1OTk6U3VuIFNlcCAxMyAyMzo1OTo1OSBXQV
    ;QgMjAyMA==","mg":"","pmg":"","stk":""},"ogn":"07034740599","opid":"011","ur":"*123*0*1
    ;","config":"ur","mk":"T","sid":"160001080733307034740599"}
    (let [resp (json/write-str {:message msg_app_error, :terminate false})]
      resp)
    (let [params {:rc "0" :ogn msisdn :opid "011" :ur input :config "ur" :mk "T" :sid sessionId}
          resp (json/write-str params)
          options  {:socket-timeout (Integer. as_connect_timeout)
                      :conn-timeout (Integer. as_read_timeout)
                      :timeout (Integer. as_connect_timeout)             ; ms
                      :query-params {:params (b64/encode (.getBytes resp))}}
            {:keys [status body error] :as trace}  @(http/get as_url options)]
        (try
          (if error
            (let [error-msg (condp instance? error
                              ConnectException ":timeout-on-connect"
                              SocketTimeoutException ":timeout-on-read")
                  resp {:message msg_app_error, :terminate false}]
              (errorf "Connection exception %s|%s" error-msg trace)
              resp)

            (if (not-empty body)
              ;{"jmgObj":{"tk":"MDcwMzQ3NDA1OTk6TW9uIFNlcCAxNCAyMzo1OTo1OSBXQVQgMjA
              ;yMA==","mg":"","pmg":"","stk":""},"code":200,"data":"Select spend limit\r\n1. N500\r\n2.
              ;N1000\r\n3. N2000\r\n4. N3000\r\n5. Another Amount","leaf":false,"ur":"*123*0"}

              (let [jsonObj (json/read-str body :key-fn keyword)
                    {:keys [jmgObj code data leaf ur]} jsonObj]
                (infof "Parsed response [%s|%s]" msisdn jsonObj)
                (if (or (nil? jsonObj) (empty? jsonObj))
                  {:message msg_app_error, :terminate false}
                  {:message data :terminate leaf}))

              {:message msg_app_error, :terminate false}))
          (catch Exception e
            (error "Exception: Error calling AS:" msisdn "|" (.printStackTrace e) )
            {:message msg_app_error, :terminate false})))))
