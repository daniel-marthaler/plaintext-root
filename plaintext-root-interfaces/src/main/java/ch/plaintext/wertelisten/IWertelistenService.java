package ch.plaintext.wertelisten;

import java.util.List;

/**
 * Service interface for managing value lists (Wertelisten).
 * Value lists provide standardized dropdown options throughout the application.
 * Each list is identified by a key and belongs to a specific mandat (tenant).
 */
public interface IWertelistenService {

    /**
     * Gets all values for a specific value list key and mandat.
     *
     * @param key    The unique identifier for the value list (e.g., "fahrzeug.art", "kontakt.anrede")
     * @param mandat The mandat/tenant identifier
     * @return List of values in sorted order, empty list if not found
     */
    List<String> getWerte(String key, String mandat);

    /**
     * Gets all values for a specific value list key for the current user's mandat.
     *
     * @param key The unique identifier for the value list
     * @return List of values in sorted order, empty list if not found
     */
    List<String> getWerte(String key);

    /**
     * Gets all available value list keys for a specific mandat.
     *
     * @param mandat The mandat/tenant identifier
     * @return List of all keys
     */
    List<String> getAllKeys(String mandat);

    /**
     * Gets all available value list keys for the current user's mandat.
     *
     * @return List of all keys
     */
    List<String> getAllKeys();

    /**
     * Creates or updates a value list.
     *
     * @param key    The unique identifier for the value list
     * @param mandat The mandat/tenant identifier
     * @param values The list of values to store
     */
    void saveWerteliste(String key, String mandat, List<String> values);

    /**
     * Deletes a value list.
     *
     * @param key    The unique identifier for the value list
     * @param mandat The mandat/tenant identifier
     */
    void deleteWerteliste(String key, String mandat);

    /**
     * Checks if a value list exists.
     *
     * @param key    The unique identifier for the value list
     * @param mandat The mandat/tenant identifier
     * @return true if the list exists
     */
    boolean exists(String key, String mandat);
}
