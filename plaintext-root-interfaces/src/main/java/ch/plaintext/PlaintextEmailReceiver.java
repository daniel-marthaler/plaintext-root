package ch.plaintext;

import java.util.List;
import java.util.Optional;

/**
 * Interface for reading and retrieving emails from the system.
 * This interface provides methods to access emails stored in the database.
 */
public interface PlaintextEmailReceiver {

    /**
     * Reads a single email by its ID.
     *
     * @param emailId The ID of the email to read
     * @return Optional containing the email if found, empty otherwise
     */
    Optional<Object> readEmail(Long emailId);

    /**
     * Reads all emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to get emails for
     * @return List of emails for the mandate, ordered by creation date (newest first)
     */
    List<Object> readEmailsForMandate(String mandat);

    /**
     * Reads all queued emails (emails waiting to be sent).
     *
     * @return List of queued emails
     */
    List<Object> readQueuedEmails();

    /**
     * Reads all queued emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to get queued emails for
     * @return List of queued emails for the mandate
     */
    List<Object> readQueuedEmailsForMandate(String mandat);

    /**
     * Reads all incoming emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to get incoming emails for
     * @return List of incoming emails for the mandate
     */
    List<Object> readIncomingEmailsForMandate(String mandat);

    /**
     * Reads all outgoing emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to get outgoing emails for
     * @return List of outgoing emails for the mandate
     */
    List<Object> readOutgoingEmailsForMandate(String mandat);

    /**
     * Gets the count of queued emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to count emails for
     * @return Number of queued emails
     */
    long getQueuedCount(String mandat);

    /**
     * Gets the count of sent emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to count emails for
     * @return Number of sent emails
     */
    long getSentCount(String mandat);

    /**
     * Gets the count of failed emails for a specific mandate.
     *
     * @param mandat The mandate/tenant to count emails for
     * @return Number of failed emails
     */
    long getFailedCount(String mandat);

    /**
     * Gets the names of all email configurations for a specific mandate.
     *
     * @param mandat The mandate/tenant to get config names for
     * @return List of config names
     */
    List<String> getConfigNamesForMandate(String mandat);
}
