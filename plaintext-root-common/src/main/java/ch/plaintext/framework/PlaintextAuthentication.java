package ch.plaintext.framework;

import java.util.List;
import java.util.Set;

public interface PlaintextAuthentication {

    Boolean isAutenticated();

    Boolean hasRole(String role);

    List<PlaintextUser> getAllUsers();

    void saveUser(PlaintextUser user);

    PlaintextUser loadUser();

    PlaintextUser createNewUser();

    String getUsername();

    PlaintextUser getUser(boolean... force);

    Set<String> getAllRoles();

}
