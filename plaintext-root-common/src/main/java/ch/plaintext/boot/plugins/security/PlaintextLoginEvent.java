package ch.plaintext.boot.plugins.security;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a user successfully logs in
 */
public class PlaintextLoginEvent extends ApplicationEvent {
    
    private final String userEmail;
    private final Long userId;
    private final String userName;
    private final String mandat;
    private final String requestBaseUrl;

    public PlaintextLoginEvent(Object source, String userEmail, Long userId, String userName, String mandat) {
        this(source, userEmail, userId, userName, mandat, null);
    }

    public PlaintextLoginEvent(Object source, String userEmail, Long userId, String userName, String mandat, String requestBaseUrl) {
        super(source);
        this.userEmail = userEmail;
        this.userId = userId;
        this.userName = userName;
        this.mandat = mandat;
        this.requestBaseUrl = requestBaseUrl;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public String getMandat() {
        return mandat;
    }

    public String getRequestBaseUrl() {
        return requestBaseUrl;
    }
}