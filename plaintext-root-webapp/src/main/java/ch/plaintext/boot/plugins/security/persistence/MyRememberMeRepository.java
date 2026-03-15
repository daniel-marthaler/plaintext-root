package ch.plaintext.boot.plugins.security.persistence;

import ch.plaintext.boot.plugins.security.model.MyRememberMe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MyRememberMeRepository extends JpaRepository<MyRememberMe, String> {

    MyRememberMe findByUsername(String name);

    List<MyRememberMe> findAllByUsername(String username);

    @Transactional
    void deleteAllByUsername(String username);

}