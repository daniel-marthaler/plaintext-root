/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext;

import java.util.List;

/**
 * Interface for components that want to be notified when new emails arrive.
 * Implementations can register themselves and will be called whenever the
 * EmailReceiveService processes incoming emails.
 */
public interface PlaintextIncomingEmailListener {

    /**
     * Called when a new email has been received.
     *
     * @param emailId The ID of the received email
     * @param mandat The mandate/tenant for which the email was received
     * @param configName The name of the email configuration that received the email
     */
    void onEmailReceived(Long emailId, String mandat, String configName);

    /**
     * Returns the display name of this listener for logging purposes.
     *
     * @return A human-readable name
     */
    String getListenerName();

    /**
     * Returns the list of email configuration names this listener is interested in.
     * Return an empty list or null to listen to all configurations.
     *
     * @return List of configuration names to listen to, or empty/null for all
     */
    default List<String> getConfigNamesToListenTo() {
        return null; // Listen to all by default
    }
}
