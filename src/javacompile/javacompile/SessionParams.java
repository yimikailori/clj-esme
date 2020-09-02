package javacompile;

import java.util.Hashtable;
import java.util.Map;
/**
 * Created by Yimika on 27/07/2016.
 */
public class SessionParams {

    Map session_data = null;

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
        String sessionId = (String) this.session_data.get(msisdn);
        return sessionId;
    }

    public void clearSession(String msisdn){
        this.session_data.remove(msisdn);
    }

    public boolean sessionExists(String msisdn){
        boolean exists = this.session_data.containsKey(msisdn);
        return exists;
    }


}
