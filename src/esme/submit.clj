(ns esme.submit
  (:use [clojure.tools.logging])
  (:require [esme.callapp :as ca]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:import org.smpp.Session
           (org.smpp.pdu SubmitSM PDU)
           (org.smpp.util ByteBuffer)
           (org.smpp Data)))

(defn ussdparams [request act ussd_service_tag_param ussd_service_tag session msisdn msg]
  (let [action (cond (= act "C") 2
                     ( = act "S") 17
                     (= act "N") 3
                     :else "32")]
    (.setServiceType request (str "USSD"))
    (.setSourceAddr request msisdn)
    (.setDestAddr request msisdn)
    (.setReplaceIfPresentFlag request (byte 0))
    (.setShortMessage request msg)
    (.setScheduleDeliveryTime request "")
    (.setValidityPeriod request "")
    (.setEsmClass request (byte 0))
    (.setProtocolId request (byte 0))
    (.setPriorityFlag request (byte 0))
    (.setRegisteredDelivery request (byte 0))
    (.setDataCoding request (byte 0))
    (.setSmDefaultMsgId request (byte 0))
    (.assignSequenceNumber request true)
    (.appendByte ussd_service_tag (reset! ussd_service_tag_param action))
    (.setExtraOptional request (Data/OPT_PAR_USSD_SER_OP) ussd_service_tag)
    (.submit session request)
    (debug "Submit request |"(.debugString request)"|" action)
    ["recieved" nil]))

(defn submit
  [^PDU pdu ^Session session ^String type newini as_url msg_esme_error as_connect_timeout as_read_timeout msg_as_timeout & args]

    (debug "Preparing SubmitSm PDU, Request type: " type )
    (let [request (SubmitSM.)
          msisdn (.getAddress (.getSourceAddr pdu))
          input (.getShortMessage pdu)
          ussd_service_tag (ByteBuffer.)
          as_request_type (atom 0)
          ussd_service_tag_param (atom 20)]
      (try
        (debug "Request Details: MSISDN:" msisdn "INPUT:" input )
        (let [s1 (cond (= type "0501000101") (let [ses (.newSession newini msisdn)]
                                               (debug "New Session APP Request Params:" msisdn "|" input "|" ses "|"(str @as_request_type))
                                               ses)
                       (= type "0501000112") (let [_ (reset! as_request_type 1)
                                                   ses (.getSessionId newini msisdn)]
                                               (debug "Continue Session APP Request Params:" msisdn "|" input "|" ses "|" (str @as_request_type))
                                               ses)
                       :else (let [ses (.newSession newini msisdn)]
                               (debug "Unknown Session APP Request Params:" msisdn "|" input "|" ses "|" (str @as_request_type))
                               ses))]
          (let [start (System/currentTimeMillis)
                as_resp (ca/callAPP msisdn input s1 @as_request_type as_url
                                    as_connect_timeout as_read_timeout msg_as_timeout)
                jsonObj (json/read-str as_resp :key-fn keyword)
                msg (:message jsonObj )
                action (:action jsonObj)
                duration (- (System/currentTimeMillis) start)]
            (debugf "callApp [%s|%s|%s|%s]" duration (str/trim (str/join "\\n" (str/split-lines msg))) msisdn action)
            (ussdparams request action ussd_service_tag_param ussd_service_tag session msisdn msg)))
    (catch Exception e
      (error "Exception: Error while submiting: " (.printStackTrace e) )
      (ussdparams request "S" ussd_service_tag_param ussd_service_tag session msisdn msg_esme_error)
     ))))
