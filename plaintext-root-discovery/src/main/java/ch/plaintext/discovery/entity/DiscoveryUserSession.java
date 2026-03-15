package ch.plaintext.discovery.entity;

import ch.emad.framework.SuperModel;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Tracks user login sessions across different app instances
 */
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "discovery_user_session")
public class DiscoveryUserSession extends SuperModel {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private DiscoveryApp app;
    
    @Column(name = "user_email", length = 255, nullable = false)
    private String userEmail; // Email for cross-app user matching
    
    @Column(name = "user_id")
    private Long userId; // Local user ID in this app (nullable for remote sessions)
    
    @Column(name = "user_name", length = 200)
    private String userName; // Display name
    
    @Column(name = "logged_in_at", nullable = false)
    private LocalDateTime loggedInAt; // When user logged into this app
    
    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt; // Last seen activity
    
    @Column(name = "session_active", nullable = false)
    private Boolean sessionActive = true; // Still active session
    
    @Column(name = "login_token", length = 500)
    private String loginToken; // Temporary cross-app login token
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt; // Token expiration
    
    @Column(name = "token_used", nullable = false)
    private Boolean tokenUsed = false; // Token has been consumed
}