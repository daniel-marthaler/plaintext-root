package ch.plaintext.boot.plugins.objstore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
public class SimpleStorableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30000)
    @Getter @Setter
    @Convert(converter = SimpleStorableConverter.class)
    private SimpleStorable myObject;


    @Column(unique = true, nullable = false)
    private String uniqueId;

    @PrePersist
    @PreUpdate
    private void syncUniqueId() {
        this.uniqueId = myObject.getUniqueId();
    }

}