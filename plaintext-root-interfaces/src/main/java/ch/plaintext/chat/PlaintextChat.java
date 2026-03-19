/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

/**
 * Interface for sending direct messages to users
 * This can be used by other modules (e.g., Schiffeversenken) to send invitations or messages
 *
 * @author info@plaintext.ch
 * @since 2026
 */
public interface PlaintextChat {

    /**
     * Send a direct message to a user
     *
     * @param recipientEmail The email address of the recipient
     * @param message The message text (can contain links like /schiffeversenken.htm?anfrage=dcjdcidjcdicj)
     * @param senderEmail The email address of the sender
     */
    void sendDirectMessage(String recipientEmail, String message, String senderEmail);

    /**
     * Get all unread direct messages for a user
     *
     * @param userEmail The email address of the user
     * @return List of unread direct messages
     */
    java.util.List<DirectMessageDTO> getUnreadDirectMessages(String userEmail);

    /**
     * Mark a direct message as read
     *
     * @param messageId The ID of the message
     * @param userEmail The email address of the user
     */
    void markAsRead(Long messageId, String userEmail);
}
