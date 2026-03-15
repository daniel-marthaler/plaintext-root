package ch.plaintext.sessions.repository;

import ch.plaintext.sessions.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    List<UserSession> findBySessionId(String sessionId);

    @Query("SELECT u FROM UserSession u WHERE u.sessionId = :sessionId ORDER BY u.id DESC")
    List<UserSession> findAllBySessionIdOrderByIdDesc(@Param("sessionId") String sessionId);

    List<UserSession> findByUserIdAndActive(Long userId, Boolean active);

    List<UserSession> findAllByActive(Boolean active);

    List<UserSession> findByMandatAndActive(String mandat, Boolean active);
}
