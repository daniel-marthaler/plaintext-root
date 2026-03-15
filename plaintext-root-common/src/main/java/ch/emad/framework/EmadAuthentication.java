package ch.emad.framework;

import java.util.List;
import java.util.Set;

public interface EmadAuthentication {

    Boolean isAutenticated();

    Boolean hasRole(String role);

    List<EmadUser> getAllUsers();

    void saveUser(EmadUser user);

    EmadUser loadUser();

    EmadUser createNewUser();

    String getUsername();

    EmadUser getUser(boolean... force);

    Set<String> getAllRoles();

}
