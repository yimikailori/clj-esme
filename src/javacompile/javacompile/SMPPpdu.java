package javacompile;

import org.smpp.ServerPDUEvent;
import org.smpp.ServerPDUEventListener;
import org.smpp.Session;
import org.smpp.SmppObject;
import org.smpp.pdu.PDU;
import org.smpp.util.Queue;

/**
 * Created by Yimika on 07/07/2016.
 */

    public class SMPPpdu extends SmppObject implements ServerPDUEventListener {
        Session session;
        Queue requestEvents = new Queue();



    public SMPPpdu(Session session) {
            this.session = session;
        }

        public void handleEvent(ServerPDUEvent event) {
            PDU pdu = event.getPDU();
            if(pdu.isRequest()) {
                System.out.println("async request received, enqueuing " + pdu.debugString());

                //async request received, enqueuing (deliver: (pdu: 56 5 0 1) (addr: 1 1 2348156545907)  (addr: 0 0 )  (sm: msg: *578#)  (opt: ) (extraopt: (oct: (tlv: 1281) 01)  ) )
                if(pdu.getCommandId() == 5) {
                    Queue var3 = this.requestEvents;
                    synchronized (this.requestEvents) {
                        this.requestEvents.enqueue(event);
                        this.requestEvents.notify();
                    }


                }
            } else if(pdu.isResponse()) {
                System.out.println("async response received " + pdu.debugString());
            } else {
                System.out.println("pdu of unknown class (not request nor response) received, discarding " + pdu.debugString());
            }

        }

        public ServerPDUEvent getRequestEvent(long timeout) {
            ServerPDUEvent pduEvent = null;
            Queue var4 = this.requestEvents;
            synchronized(this.requestEvents) {
                if(this.requestEvents.isEmpty()) {
                    try {
                        this.requestEvents.wait(timeout);
                    } catch (InterruptedException var7) {
                        ;
                    }
                }

                if(!this.requestEvents.isEmpty()) {
                    pduEvent = (ServerPDUEvent)this.requestEvents.dequeue();
                }

                return pduEvent;
            }
        }
    }
