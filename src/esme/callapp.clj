(ns esme.callapp
  (:use [clojure.tools.logging])
  (:require [clojure.data.json :as json])
  (:require [clj-http.client :as http]
            [clojure.string :as str])
  (:import (java.net ConnectException SocketTimeoutException)))


(defn callAPP
  [msisdn  input sessionId as_request_type as_url
   as_connect_timeout as_read_timeout msg_as_timeout]

  (if (every? empty? [msisdn input sessionId as_request_type])
    (let [resp (json/write-str {:message msg_as_timeout, :action "S"})]
      resp)
    (let [options  {:socket-timeout (Integer. as_connect_timeout)
										:conn-timeout (Integer. as_read_timeout)
										:timeout (Integer. as_connect_timeout)             ; ms
										:query-params {:sub msisdn :sid sessionId :state as_request_type :msg input}}
            {:keys [status body error] :as trace}  @(http/get as_url options)]
        (try
          (if error
            (let [error-msg (condp instance? error
                              ConnectException ":timeout-on-connect"
                              SocketTimeoutException ":timeout-on-read")
                  resp (json/write-str {:message msg_as_timeout, :action "S"})]
              (errorf "Connection exception %s|%s" error-msg trace)
              resp)

            (if (not-empty body)
              (let [jsonObj (json/read-str body :key-fn keyword)
                    parse-msg (:message jsonObj )]
                (infof "Parsed response [%s|%s]" msisdn body)
                (if (or (nil? parse-msg) (empty? parse-msg))
                  (json/write-str {:message msg_as_timeout, :action "S"})
                  body))

              (let [resp (json/write-str {:message msg_as_timeout, :action "S"})]
                resp)))
          (catch Exception e
            (error "Exception: Error calling AS:" msisdn "|" (.printStackTrace e) )
            (let [resp (json/write-str {:message msg_as_timeout, :action "S"})]
              resp))))))
