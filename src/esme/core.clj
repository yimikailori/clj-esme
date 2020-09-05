
(ns ^{:doc    "The esme  written in clojure "
      :author "yilori"}
esme.core
  (:gen-class :main true)
  (:use [clojure.tools.logging]
        [clojure.tools.cli :only (cli)]
        [clojure-ini.core :as prop])
  (:require [esme.processRequest :as pr]
            [clojure.core.async :refer [go] :as async])
  (:import org.smpp.TCPIPConnection)
  (:import org.smpp.pdu.BindTransciever)
  (:import org.smpp.util.Queue)
  (:import (org.smpp Data ServerPDUEvent Session)
           (javacompile SMPPpdu)
           (javacompile SessionParams)
           (java.util Timer TimerTask)))

(def checkBind (atom 0))


(defn task [session attemptBind]
  (try
    (debug "enquireLink" session)
    (if (= @checkBind 1)
      (let [newbind(BindTransciever.)
            _ (reset! checkBind 0)]
        (infof "reattempting to rebind = %s" @checkBind)
        (attemptBind newbind))
      (do
        (.enquireLink session)
        (reset! checkBind 1)))

    (catch InterruptedException e
      (let [_(error "task enquireLink InterruptedException"  e)
            newbind(BindTransciever.)
            _ (swap! checkBind inc)]
        (info "reattempting Interrupted, rebind =" @checkBind )
        (attemptBind newbind)))))


#_(defn set-interval [callback ms]
 (async/go (while (not (Thread/interrupted)) (do (Thread/sleep ms) (callback)))))




#_(defn SessionParams
    [msisdn newSession getSessionId clearSession]
    (let [session_data (Hashtable.)
          ^JenkinsHash hash (JenkinsHash.)]
      (if (not-empty newSession)
        (let [sessionId (.hashCode hash)]
          (.put session_data msisdn sessionId)
          (.toString sessionId))
        (if (not-empty getSessionId)
          (let [sessionId (.get session_data msisdn)]
            (.toString sessionId))
          (if (not-empty clearSession)
            (.remove session_data msisdn))))))




(defn-
  ;Declare properties parameters from config"
  config [con] (prop/read-ini con :keywordize? true :comment-char \#))

(defn -main
  [& args]
  (let [[opts args banner]
        (cli args
             ["-h" "--help" "Show help" :flag true :default false]
             ["-c" "--config" "REQUIRED: Path to configuration properties"]
             )]
    (info "Service starting...")
    (let [ config_details (:config opts)
          {ussd_ip          :host
           ussd_port        :port
           ussd_id          :ussd-username
           ussd_password    :ussd-password
           ussd_system_type :system-type
           as_url           :as-url
           msg_esme_error   :msg-esme-error
           as_connect_timeout :as-connect-timeout
           as_read_timeout   :as-read-timeout
           msg_as_timeout    :msg-as-timeout} (config config_details)]

        (try
          (info (str "ussd_ip and port is " ussd_ip ":" ussd_port))
          (info (str "APP URL " as_url))
          (let [bindReq (BindTransciever.)
                connect (TCPIPConnection. ussd_ip (Integer. ussd_port))
                session (Session. connect)
                attemptBind (fn [bindReq]
                              (do
                                (.setReceiveTimeout connect 5000)

                                (info (str "Attempting to bind with id:" ussd_id ", bindcount ="@checkBind))
                                (.setSystemId bindReq ussd_id)

                                (info (str "Attempting to bind with password: *********" ))
                                (.setPassword bindReq ussd_password)

                                (info (str "Attempting to bind with system_type: " ussd_system_type))
                                (.setSystemType bindReq ussd_system_type)

                                (.setInterfaceVersion bindReq (byte 0x34))

                                (.setAddressRange bindReq (byte 0) (byte 0) (str ""))

                                (infof "Trying to bind now (%s)" (.debugString bindReq))))
                unbind (fn []
                         (let [response (.unbind session)
                               _ (info "Ubind Response" (.debugString response))]
                           (info "Service shutting down")))
                _  (attemptBind bindReq)
                requestEvents (Queue.)
                newini (SessionParams.)
                pduListener (proxy [SMPPpdu] [session]
                              (handleEvent [#^ServerPDUEvent event]
                                (let [pdu (.getPDU event)]
                                    ;;;;;;;;;;;;;;;;
                                    (if (.isRequest pdu)
                                      (async/go
                                        (let [[refState refmsg] (pr/processRequest pdu session newini as_url msg_esme_error
                                                                                   as_connect_timeout as_read_timeout msg_as_timeout checkBind)]
                                          #_(when (nil? refState)
                                              ;;refMsg => Not connected ;;Broken pipe
                                              (errorf "error state[%s]"refmsg)
                                              (Thread/sleep 3000)
                                              (let [newbind(BindTransciever.)
                                                    _ (swap! checkBind inc)]
                                                (info "reattempting to rebind due to refmsg =" @checkBind )
                                                (attemptBind newbind)))))
                                      (if (.isResponse pdu)
                                        (do
                                          (if (== (.getCommandStatus pdu) (Data/ESME_RSUBMITFAIL ))
                                            (error "asynchronous response Failed |" (.debugString pdu))
                                            (do
                                              (reset! checkBind 0)
                                              (debug "asynchronous response received |" (.debugString pdu)))))
                                        (warn "asynchronous unknown pdu response received |" (.debugString pdu)))
                                      ))))
                bindResp (.bind session bindReq pduListener)]
            (info "Bind response: " (.debugString bindResp))
            (info "Command Status: " (.getCommandStatus bindResp))
            (if (== (.getCommandStatus bindResp) (Data/ESME_ROK))
              (do
                (info "Bound to USSD Successfully.")

                (.addShutdownHook (Runtime/getRuntime) (Thread. (unbind)))
                ;;send enquirelink every 1secs
                ;;(set-interval #(task session attemptBind checkBind) 1000)
                (doto (Timer. "enquireLink" true)
                  (.scheduleAtFixedRate (proxy [TimerTask] []
                                          (run [] (task session attemptBind)))
                                        5000 5000)))
              (do
                (error "Sorry couldnt bind to USSD gateway")
                (System/exit 0)))


            ;  (TLV. ussd_service_opt)
            ) (catch Exception e
                (error "Error is: " (.getMessage e) e))))))




;(-main "--config=C:\\Users\\Yimika\\IdeaProjects\\EsmeSmpp_trial\\esmeprop.conf")


