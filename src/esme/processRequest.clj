(ns
  esme.processRequest
  (:use [clojure.tools.logging])
  (:import (org.smpp Data)
           (org.smpp.pdu Request DeliverSM)
           (org.smpp.pdu.tlv TLV))
  (:import org.smpp.Session)
  (:import org.smpp.TCPIPConnection)
  (:import org.smpp.pdu.BindTransciever)
  (:import javacompile.SessionParams)
  (:require [esme.submit :as sm]))


(defn processRequest
  [pdu session newini as_url msg_esme_error as_connect_timeout as_read_timeout msg_app_error checkBind]
  ; It means if commandID pdu is equals 5
  ; (if (=(.getCommandId pdu) 5)
  (when (=(.getCommandId pdu) (Data/DELIVER_SM))
    (do
      (try
        (info "async request received, " (.debugString pdu) "| Command ID |" (.getCommandId pdu))
        (let [^TLV ussd_service_tag (.getExtraOptional pdu (Data/OPT_PAR_USSD_SER_OP))]
          (info "USSD OPT Received |" (.getHexDump (.getData ussd_service_tag)))
          (if (or (or (= (.getHexDump (.getData ussd_service_tag)) (str "0501000101"))
                      (= (.getHexDump (.getData ussd_service_tag)) (str "0501000118")))
                  (= (.getHexDump (.getData ussd_service_tag)) (str "0501000112")))

            ;;if pdu is message from customer
            (let [resp (.getResponse ^Request pdu)]
              (info "Received SM USSD_SERVICE_TYPE | " (.getHexDump (.getData ussd_service_tag)) "| Call AS and Respond")
              (.respond session resp)
              ;;submit function here and call APP here
              (sm/submit pdu session (.getHexDump (.getData ussd_service_tag)) newini as_url msg_esme_error
                         as_connect_timeout as_read_timeout msg_app_error))

            ;else clear or end session
            (let [msisdn (.getAddress (.getSourceAddr pdu))
                  resp (.getResponse ^Request pdu)]
              (.clearSession newini msisdn)
              ;;this SessionParams clears session
              ;;(SessionParams msisdn "" "" "true")
              (.respond session resp)
              (info "Received SM USSD_SERVICE_TYPE |" (.getHexDump (.getData ussd_service_tag)) "|" msisdn
                    "| End Session and Respond")
              ["recieved" nil])))
        (catch Exception e
          (do
            (error "Exception: Something bad happened =>" (.printStackTrace e))
            [nil (.getMessage e)])))
      ;(debug "Request |" (.debugString pdu) "| Command ID |" (.getCommandId pdu))
      )))
