package ch.plaintext.boot.menu;

/**
 * Interface for security/role checking.
 * Implement this interface to provide custom security logic for menu items.
 */
public interface SecurityProvider {

    /**
     * Check if the current user has the specified role
     * @param role the role to check
     * @return true if the user has the role, false otherwise
     */
    boolean hasRole(String role);

    /**
     * Check if security is enabled
     * @return true if security is enabled, false otherwise
     */
    default boolean isSecurityEnabled() {
        return true;
    }
}
