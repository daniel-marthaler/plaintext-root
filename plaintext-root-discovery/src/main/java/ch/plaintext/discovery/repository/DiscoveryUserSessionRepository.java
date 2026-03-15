package ch.plaintext.discovery.repository;

import ch.plaintext.discovery.entity.DiscoveryUserSession;
import ch.plaintext.discovery.entity.DiscoveryApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveryUserSessionRepository extends JpaRepository<DiscoveryUserSession, Long> {
    
    List<DiscoveryUserSession> findByUserEmailAndSessionActiveTrue(String userEmail);
    
    Optional<DiscoveryUserSession> findByAppAndUserEmailAndSessionActiveTrue(DiscoveryApp app, String userEmail);
    
    Optional<DiscoveryUserSession> findByLoginTokenAndTokenUsedFalse(String loginToken);
    
    List<DiscoveryUserSession> findBySessionActiveTrueAndLastActivityAtAfter(LocalDateTime since);
    
    @Query("SELECT DISTINCT s.userEmail FROM DiscoveryUserSession s WHERE s.sessionActive = true")
    List<String> findActiveUserEmails();
    
    List<DiscoveryUserSession> findByAppAndSessionActiveTrue(DiscoveryApp app);

    List<DiscoveryUserSession> findByApp(DiscoveryApp app);
    
    @Query("SELECT s FROM DiscoveryUserSession s WHERE s.tokenExpiresAt < ?1 AND s.tokenUsed = false")
    List<DiscoveryUserSession> findExpiredUnusedTokens(LocalDateTime now);
}