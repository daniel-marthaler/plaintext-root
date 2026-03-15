package ch.plaintext.sessions;

import org.springframework.security.core.Authentication;

public interface ISessionAuditService {

    void updateOrCreate(Long userId, String sessionId, Authentication authentication, String userAgent);

    void commit(Long userId);
}
