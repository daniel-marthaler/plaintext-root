package ch.plaintext.boot.plugins.security.model;

import ch.plaintext.boot.plugins.security.helpers.MyUserSetConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class MyUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password = "";
    private String startpage = "";
    private String autologinKey = "";

    @Convert(converter = MyUserSetConverter.class)
    private Set<String> roles = new HashSet<>();

    public void addRole(String role) {
        roles.add(role);
    }

    public void removeRole(String role) {
        roles.remove(role);
    }

    @Transient
    public String getMandat() {
        if (roles == null) {
            return null;
        }
        for (String role : roles) {
            if (role != null && role.toUpperCase().startsWith("PROPERTY_MANDAT_")) {
                return role.substring("PROPERTY_MANDAT_".length()).toLowerCase();
            }
        }
        return null;
    }

    public void setMandat(String mandat) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        // Remove any existing mandat role
        roles.removeIf(role -> role != null && role.toUpperCase().startsWith("PROPERTY_MANDAT_"));

        // Add new mandat role if mandat is not null or empty
        if (mandat != null && !mandat.trim().isEmpty()) {
            roles.add("PROPERTY_MANDAT_" + mandat.toUpperCase());
        }
    }

}
