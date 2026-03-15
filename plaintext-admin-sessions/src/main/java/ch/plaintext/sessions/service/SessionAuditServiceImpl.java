package ch.plaintext.sessions.service;

import ch.plaintext.sessions.ISessionAuditService;
import ch.plaintext.sessions.entity.UserSession;
import ch.plaintext.sessions.repository.UserSessionRepository;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Named("sessionAuditService")
@Slf4j
public class SessionAuditServiceImpl implements ISessionAuditService {

    private final UserSessionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public SessionAuditServiceImpl(UserSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void updateOrCreate(Long userId, String sessionId, Authentication authentication, String userAgent) {
        if (userId == null || sessionId == null) {
            log.warn("updateOrCreate called with null userId or sessionId");
            return;
        }

        try {
            List<UserSession> existingSessions = repository.findBySessionId(sessionId);
            UserSession session;

            if (!existingSessions.isEmpty()) {
                // Handle duplicates defensively: use the most recent one and delete the rest
                if (existingSessions.size() > 1) {
                    log.warn("⚠️ Found {} duplicate sessions for sessionId={}. Cleaning up duplicates.",
                        existingSessions.size(), sessionId);

                    // Sort by ID descending to get the most recent
                    existingSessions.sort((a, b) -> Long.compare(b.getId(), a.getId()));

                    // Keep the first one (most recent), delete the rest
                    session = existingSessions.get(0);
                    for (int i = 1; i < existingSessions.size(); i++) {
                        repository.delete(existingSessions.get(i));
                        log.debug("Deleted duplicate session with ID={}", existingSessions.get(i).getId());
                    }
                } else {
                    session = existingSessions.get(0);
                }

                // Update existing session
                session.setLastActivityTime(LocalDateTime.now());
                session.setLastModifiedDate(LocalDateTime.now());
                repository.save(session);
            } else {
                // Create new session
                session = new UserSession();
                session.setUserId(userId);
                session.setSessionId(sessionId);
                session.setLoginTime(LocalDateTime.now());
                session.setLastActivityTime(LocalDateTime.now());
                session.setActive(true);

                // Extract user info from authentication
                session.setUsername(authentication != null ? authentication.getName() : "unknown");
                // Extract mandat from authorities (format: PROPERTY_MANDAT_xxx)
                String mandat = "unknown";
                if (authentication != null && authentication.getAuthorities() != null) {
                    mandat = authentication.getAuthorities().stream()
                        .map(auth -> auth.getAuthority())
                        .filter(auth -> auth.startsWith("PROPERTY_MANDAT_"))
                        .map(auth -> auth.substring("PROPERTY_MANDAT_".length()))
                        .findFirst()
                        .orElse("unknown");
                }
                session.setMandat(mandat);

                session.setUserAgent(userAgent != null ? userAgent : "");

                try {
                    repository.save(session);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Race condition: another thread created the session between our check and save
                    log.debug("Race condition detected for sessionId={}, reloading existing session", sessionId);

                    // Clear the entity manager to prevent "null identifier" error
                    // The failed save left a detached entity with no ID in the session
                    entityManager.clear();

                    existingSessions = repository.findBySessionId(sessionId);
                    if (!existingSessions.isEmpty()) {
                        session = existingSessions.get(0);
                        session.setLastActivityTime(LocalDateTime.now());
                        session.setLastModifiedDate(LocalDateTime.now());
                        repository.save(session);
                    } else {
                        throw e; // Should not happen, but re-throw if it does
                    }
                }
            }

            log.debug("Session updated/created: userId={}, sessionId={}", userId, sessionId);
        } catch (Exception e) {
            log.error("Error updating/creating session: userId={}, sessionId={}", userId, sessionId, e);
        }
    }

    @Override
    @Transactional
    public void commit(Long userId) {
        if (userId == null) {
            log.warn("commit called with null userId");
            return;
        }

        try {
            List<UserSession> activeSessions = repository.findByUserIdAndActive(userId, true);
            for (UserSession session : activeSessions) {
                session.setActive(false);
                repository.save(session);
            }
            log.debug("Marked {} sessions as inactive for userId={}", activeSessions.size(), userId);
        } catch (Exception e) {
            log.error("Error committing sessions for userId={}", userId, e);
        }
    }

    public List<UserSession> getAllActiveSessions() {
        try {
            return repository.findAllByActive(true);
        } catch (Exception e) {
            log.error("Error getting all active sessions", e);
            return List.of();
        }
    }

    public List<UserSession> getActiveSessionsByMandat(String mandat) {
        if (mandat == null || mandat.trim().isEmpty()) {
            log.warn("getActiveSessionsByMandat called with null or empty mandat");
            return List.of();
        }

        try {
            return repository.findByMandatAndActive(mandat, true);
        } catch (Exception e) {
            log.error("Error getting active sessions for mandat={}", mandat, e);
            return List.of();
        }
    }

    @Transactional
    public void forceLogout(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            log.warn("forceLogout called with null or empty sessionId");
            return;
        }

        try {
            List<UserSession> sessions = repository.findBySessionId(sessionId);
            if (!sessions.isEmpty()) {
                for (UserSession userSession : sessions) {
                    userSession.setActive(false);
                    repository.save(userSession);
                    log.info("Forced logout for sessionId={}, username={}", sessionId, userSession.getUsername());
                }
                if (sessions.size() > 1) {
                    log.warn("⚠️ Found and logged out {} duplicate sessions for sessionId={}", sessions.size(), sessionId);
                }
            } else {
                log.warn("Session not found for sessionId={}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error forcing logout for sessionId={}", sessionId, e);
        }
    }
}
