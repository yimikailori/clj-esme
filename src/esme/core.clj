
(ns ^{:doc    "The esme  written in clojure "
      :author "yilori"}
esme.core
  (:gen-class :main true)
  (:use
    [clojure.tools.cli :only (cli)])
  (:require [esme.processRequest :as pr]
            [esme.utils :as utils]
            [clojure.core.async :refer [go] :as async]
            [clojure.tools.logging :as log])
  (:import org.smpp.TCPIPConnection)
  (:import org.smpp.pdu.BindTransciever)
  (:import org.smpp.util.Queue)
  (:import org.smpp.util.TerminatingZeroNotFoundException
           (org.smpp Data ServerPDUEvent Session)
           (javacompile SMPPpdu)
           (javacompile SessionParams)
           (java.util Hashtable Date Timer TimerTask)
           (java.text SimpleDateFormat)))



(defn task [session attemptBind checkBind]
  (try
    (log/debug "enquireLink" session)
    (.enquireLink session)
    (catch InterruptedException e
      (let [_(log/error "task enquireLink"  e)
            newbind(BindTransciever.)
            _ (swap! checkBind inc)]
        (log/info "reattempting to rebind =" @checkBind )
        (attemptBind newbind)))))


#_(defn set-interval [callback ms]
 (async/go (while (not (Thread/interrupted)) (do (Thread/sleep ms) (callback)))))




(comment
  (defn SessionParams
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
            (.remove session_data msisdn)))))))


(defn get-date [date]
  (.format (SimpleDateFormat. "yyyy-MM-dd") date))


(defn-
  ;Declare properties parameters from config"
  config [con] (utils/read-ini con :keywordize? true :comment-char \#))

(defn -main
  [& args]
  (let [fileconfig (->> (System/getProperty "ld.esme")
                        config)]
    (log/info "Service starting...")
    (log/infof "config=>%s"fileconfig)
    (let [{ussd_ip          :host
           ussd_port        :port
           ussd_id          :ussd-username
           ussd_password    :ussd-password
           ussd_system_type :system-type
           as_url           :as-url
           msg_esme_error   :msg-esme-error
           as_connect_timeout :as-connect-timeout
           as_read_timeout   :as-read-timeout
           msg_as_timeout    :msg-as-timeout} fileconfig]

        (try
          (log/info (str "ussd_ip and port is " ussd_ip ":" ussd_port))
          (log/info (str "APP URL " as_url))
          (let [bindReq (BindTransciever.)
                connect (TCPIPConnection. ussd_ip (Integer. ussd_port))
                session (Session. connect)
                checkBind (atom 0)
                attemptBind (fn [bindReq]
                              (do
                                (.setReceiveTimeout connect 20000)

                                (log/info (str "Attempting to bind with id:" ussd_id ", bindcount ="@checkBind))
                                (.setSystemId bindReq ussd_id)

                                (log/info (str "Attempting to bind with password: *********" ))
                                (.setPassword bindReq ussd_password)

                                (log/info (str "Attempting to bind with system_type: " ussd_system_type))
                                (.setSystemType bindReq ussd_system_type)

                                (.setInterfaceVersion bindReq (byte 0x34))

                                (.setAddressRange bindReq (byte 0) (byte 0) (str ""))

                                (log/infof "Trying to bind now (%s)" (.debugString bindReq))))
                _  (attemptBind bindReq)
                requestEvents (Queue.)
                newini (SessionParams.)
                pduListener (proxy [SMPPpdu] [session]
                              (handleEvent [#^ServerPDUEvent event]
                                (let [pdu (.getPDU event)]
                                    ;;;;;;;;;;;;;;;;
                                    (when (.isRequest pdu)
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
                                              (attemptBind newbind))))))
                                    (when (.isResponse pdu)
                                      (do
                                        (if (== (.getCommandStatus pdu) (Data/ESME_RSUBMITFAIL ))
                                          (log/error "asynchronous response Failed |" (.debugString pdu))
                                          (log/info "asynchronous response received |" (.debugString pdu))))))))
                bindResp (.bind session bindReq pduListener)]
            (log/info "Bind response: " (.debugString bindResp))
            (log/info "Command Status: " (.getCommandStatus bindResp))
            (if (== (.getCommandStatus bindResp) (Data/ESME_ROK))
              (do
                (log/info "Bound to USSD Successfully.")
                ;;send enquirelink every 1secs
                ;;(set-interval #(task session attemptBind checkBind) 1000)
                (doto (Timer. "enquireLink" true)
                  (.scheduleAtFixedRate (proxy [TimerTask] []
                                          (run [] (task session attemptBind checkBind)))
                                        5000 5000)))
              (do
                (log/error "Sorry couldnt bind to USSD gateway")
                (System/exit 0)))


            (.addShutdownHook (Runtime/getRuntime)
                              (Thread. (fn [] (log/info "Service shutting down...")
                                         (let [response (.unbind session)]
                                           (log/info "Ubind Response" (.debugString response))))))

            ;  (TLV. ussd_service_opt)
            ) (catch Exception e
                (log/errorf "Error is: %s" (.getMessage e) e))))))




;(-main "--config=/Users/yimika/Documents/IdeaProjects/clj-esme-ng/resources/esmeprop.conf")


