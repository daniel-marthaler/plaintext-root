package ch.plaintext.settings;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic Settings Service Interface.
 * Provides access to hierarchical settings with dot-separated keys.
 * Settings are mandat-based for multi-tenancy support.
 */
public interface ISettingsService {

    /**
     * Get a string setting value.
     */
    String getString(String key, String mandat);

    /**
     * Get a string setting value for the current user's mandat.
     */
    String getString(String key);

    /**
     * Get an integer setting value.
     */
    Integer getInt(String key, String mandat);

    /**
     * Get an integer setting value for the current user's mandat.
     */
    Integer getInt(String key);

    /**
     * Get a boolean setting value.
     */
    Boolean getBoolean(String key, String mandat);

    /**
     * Get a boolean setting value for the current user's mandat.
     */
    Boolean getBoolean(String key);

    /**
     * Get a date setting value.
     */
    LocalDateTime getDate(String key, String mandat);

    /**
     * Get a date setting value for the current user's mandat.
     */
    LocalDateTime getDate(String key);

    /**
     * Get a list setting value (comma-separated string).
     */
    List<String> getList(String key, String mandat);

    /**
     * Get a list setting value for the current user's mandat.
     */
    List<String> getList(String key);

    /**
     * Set a setting value.
     */
    void setSetting(String key, String mandat, String value, String valueType, String description);

    /**
     * Set a setting value for the current user's mandat.
     */
    void setSetting(String key, String value, String valueType, String description);

    /**
     * Delete a setting.
     */
    void deleteSetting(String key, String mandat);

    /**
     * Check if a setting exists.
     */
    boolean exists(String key, String mandat);

    /**
     * Get all keys for a mandat.
     */
    List<String> getAllKeys(String mandat);

    /**
     * Get all settings with a specific key prefix (for hierarchical navigation).
     */
    List<String> getChildKeys(String parentKey, String mandat);
}
