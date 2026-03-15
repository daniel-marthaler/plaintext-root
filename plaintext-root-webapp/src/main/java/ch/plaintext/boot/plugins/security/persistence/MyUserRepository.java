package ch.plaintext.boot.plugins.security.persistence;


import ch.plaintext.boot.plugins.security.model.MyUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyUserRepository extends JpaRepository<MyUserEntity, Long> {
    MyUserEntity findByUsername(String username);
    MyUserEntity findByAutologinKey(String autologinKey);
}

