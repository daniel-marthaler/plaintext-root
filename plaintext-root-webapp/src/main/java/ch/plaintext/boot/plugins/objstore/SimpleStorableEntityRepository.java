package ch.plaintext.boot.plugins.objstore;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SimpleStorableEntityRepository extends JpaRepository<SimpleStorableEntity, Long> {

    SimpleStorableEntity findByUniqueId(String uniqueId);

}