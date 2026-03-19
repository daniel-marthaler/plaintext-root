/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext;

/**
 * Interface for sending emails through the Plaintext email system.
 * Implementations of this interface will queue emails for delivery by the email cron job.
 *
 * @author info@plaintext.ch
 * @since 2024
 */
public interface PlaintextEmailSender {

    /**
     * Sends an email using a specific email configuration.
     * The email will be sent by the email cron job based on the specified configuration.
     *
     * @param configName the name of the email configuration to use (e.g., "maintenance", "default", "geburtstag")
     * @param to the recipient email address (required)
     * @param subject the email subject
     * @param body the email body content
     * @param html whether the body content is HTML (true) or plain text (false)
     * @return the ID of the queued email
     * @throws IllegalArgumentException if required parameters are missing or invalid
     * @throws IllegalStateException if email configuration is not found
     */
    Long sendEmail(String configName, String to, String subject, String body, boolean html);

    /**
     * Sends an email with optional CC and BCC recipients using a specific email configuration.
     *
     * @param configName the name of the email configuration to use (e.g., "maintenance", "default", "geburtstag")
     * @param to the recipient email address (required)
     * @param cc optional CC (carbon copy) recipient(s), comma-separated
     * @param bcc optional BCC (blind carbon copy) recipient(s), comma-separated
     * @param subject the email subject
     * @param body the email body content
     * @param html whether the body content is HTML (true) or plain text (false)
     * @return the ID of the queued email
     * @throws IllegalArgumentException if required parameters are missing or invalid
     * @throws IllegalStateException if email configuration is not found
     */
    Long sendEmail(String configName, String to, String cc, String bcc, String subject, String body, boolean html);
}
