package ch.plaintext.boot.plugins.security.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.Date;

@Entity
@Data
@Table(name = "persistent_logins")
public class MyRememberMe {

    @Id
    private String series;

    private String username;

    private String token;

    private Date lastUsed;


}
