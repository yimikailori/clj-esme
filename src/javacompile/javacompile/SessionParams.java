package javacompile;

import jenkinshash.JenkinsHash;

import java.util.Hashtable;
import java.util.Map;


/**
 * Created by Yimika on 27/07/2016.
 */
public class SessionParams {

    Map session_data;

    public SessionParams() {
        this.session_data = new Hashtable();
    }

    public String newSession(String msisdn){
        JenkinsHash hash = new JenkinsHash();
        String sessionId = Integer.toString(hash.hashCode());
        this.session_data.put(msisdn, sessionId);
        return sessionId;
    }

    public String getSessionId(String msisdn){
        return (String) this.session_data.get(msisdn);
    }

    public void clearSession(String msisdn){
        this.session_data.remove(msisdn);
    }

    public boolean sessionExists(String msisdn){
        return this.session_data.containsKey(msisdn);
    }

    public boolean sessionSave(String jmgMsisdn, String data){
        this.session_data.put(jmgMsisdn, data);
        return true;
    }
}
