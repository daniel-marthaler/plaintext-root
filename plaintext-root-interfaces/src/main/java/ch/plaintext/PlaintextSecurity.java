/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext;

import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Set;

/**
 * @author : mad
 * @since : 15.11.2025
 **/
public interface PlaintextSecurity {

    String getMandat();

    Set<String> getAllMandate();

    Long getId();

    String getUser();

    Authentication getAuthentication();
    /**
     * Gets the mandat for a specific user by their ID
     * @param userId User ID
     * @return The mandat string for the user, or null if user not found
     */
    String getMandatForUser(long userId);

    boolean ifGranted(String role);

    /**
     * Gets all usernames for a specific mandat
     * @param mandat The mandat to filter by
     * @return List of usernames belonging to the specified mandat
     */
    List<String> getUsersForMandat(String mandat);

    /**
     * Gets the startpage for the current user with fallback to index.html
     * If startpage is null, empty, or "N/A", returns "/index.html?faces-redirect=true"
     * @return The startpage URL with faces-redirect parameter
     */
    String getStartpageOrDefault();

    /**
     * Checks if the current user is impersonating another user
     * @return true if currently impersonating
     */
    boolean isImpersonating();

    /**
     * Starts impersonation of another user
     * @param userId The ID of the user to impersonate
     */
    void startImpersonation(Long userId);

    /**
     * Stops impersonation and returns to original user
     */
    void stopImpersonation();

    /**
     * Gets the original user ID before impersonation
     * @return Original user ID, or null if not impersonating
     */
    Long getOriginalUserId();

}