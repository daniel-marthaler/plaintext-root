package ch.plaintext.discovery.repository;

import ch.plaintext.discovery.entity.DiscoveryApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscoveryAppRepository extends JpaRepository<DiscoveryApp, Long> {
    
    Optional<DiscoveryApp> findByAppId(String appId);
    
    List<DiscoveryApp> findByActiveTrue();
    
    List<DiscoveryApp> findByEnvironment(DiscoveryApp.AppEnvironment environment);
    
    @Query("SELECT a FROM DiscoveryApp a WHERE a.active = true AND a.lastSeenAt > :since")
    List<DiscoveryApp> findActiveAppsSince(LocalDateTime since);
    
    @Query("SELECT a FROM DiscoveryApp a WHERE a.active = true AND a.lastSeenAt < :before")
    List<DiscoveryApp> findStaleApps(LocalDateTime before);

    @Query("SELECT a FROM DiscoveryApp a WHERE a.lastSeenAt < :before")
    List<DiscoveryApp> findAppsNotSeenSince(LocalDateTime before);
}