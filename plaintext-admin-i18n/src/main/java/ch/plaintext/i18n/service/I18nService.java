/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.i18n.service;

import ch.plaintext.I18nProvider;
import ch.plaintext.i18n.entity.I18nTranslation;
import ch.plaintext.i18n.repository.I18nTranslationRepository;
import ch.plaintext.settings.ISettingsService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * I18n translation service implementing the I18nProvider interface.
 * Provides caching, DB lookup, and fallback to the default label.
 */
@Service
@Slf4j
public class I18nService implements I18nProvider {

    private final I18nTranslationRepository repository;

    @Autowired(required = false)
    private ISettingsService settingsService;

    /**
     * Cache: key = "defaultLabel::languageCode", value = translated text.
     */
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public I18nService(I18nTranslationRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        loadCache();
    }

    private void loadCache() {
        cache.clear();
        List<I18nTranslation> all = repository.findAll();
        for (I18nTranslation t : all) {
            cache.put(cacheKey(t.getDefaultLabel(), t.getLanguageCode()), t.getTranslatedText());
        }
        log.info("I18n cache loaded with {} translations", all.size());
    }

    /** Prefix used for auto-generated placeholder translations. */
    public static final String UNTRANSLATED_PREFIX = "X_";

    @Override
    public String translate(String defaultLabel, String targetLanguage) {
        if (defaultLabel == null || targetLanguage == null) {
            return defaultLabel;
        }

        // "de" is the source language - no translation needed
        if ("de".equalsIgnoreCase(targetLanguage)) {
            return defaultLabel;
        }

        // Lookup in cache first
        String key = cacheKey(defaultLabel, targetLanguage);
        String cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // Fallback: lookup in DB
        Optional<I18nTranslation> found = repository.findByDefaultLabelAndLanguageCode(defaultLabel, targetLanguage);
        if (found.isPresent()) {
            String translated = found.get().getTranslatedText();
            cache.put(key, translated);
            return translated;
        }

        // No translation found: auto-create a placeholder with "X_" prefix
        String placeholder = UNTRANSLATED_PREFIX + defaultLabel;
        try {
            autoCreatePlaceholder(defaultLabel, targetLanguage, placeholder);
        } catch (Exception e) {
            log.warn("Could not auto-create i18n placeholder for label='{}', lang='{}': {}",
                    defaultLabel, targetLanguage, e.getMessage());
        }

        cache.put(key, placeholder);
        return placeholder;
    }

    /**
     * Auto-inserts a placeholder translation record so untranslated items
     * become visible in the admin UI (prefixed with "X_").
     */
    @Transactional
    public void autoCreatePlaceholder(String defaultLabel, String languageCode, String placeholder) {
        // Double-check to avoid unique constraint violations in concurrent scenarios
        Optional<I18nTranslation> existing = repository.findByDefaultLabelAndLanguageCode(defaultLabel, languageCode);
        if (existing.isPresent()) {
            return;
        }
        I18nTranslation translation = new I18nTranslation();
        translation.setDefaultLabel(defaultLabel);
        translation.setLanguageCode(languageCode);
        translation.setTranslatedText(placeholder);
        repository.save(translation);
        log.info("Auto-created i18n placeholder: label='{}', lang='{}', text='{}'", defaultLabel, languageCode, placeholder);
    }

    @Override
    public boolean isI18nEnabled() {
        if (settingsService != null) {
            try {
                Boolean enabled = settingsService.getBoolean("i18n.enabled");
                if (enabled != null) {
                    return enabled;
                }
            } catch (Exception e) {
                log.debug("Could not read i18n.enabled setting, defaulting to true");
            }
        }
        return true;
    }

    @Override
    public List<String> getAvailableLanguages() {
        if (settingsService != null) {
            try {
                List<String> langs = settingsService.getList("i18n.languages");
                if (langs != null && !langs.isEmpty()) {
                    return langs;
                }
            } catch (Exception e) {
                log.debug("Could not read i18n.languages setting, using defaults");
            }
        }
        return Arrays.asList("de", "en", "fr", "it");
    }

    @Transactional
    public I18nTranslation saveTranslation(String defaultLabel, String languageCode, String translatedText) {
        I18nTranslation translation = repository
                .findByDefaultLabelAndLanguageCode(defaultLabel, languageCode)
                .orElse(new I18nTranslation());

        translation.setDefaultLabel(defaultLabel);
        translation.setLanguageCode(languageCode);
        translation.setTranslatedText(translatedText);

        I18nTranslation saved = repository.save(translation);
        cache.put(cacheKey(defaultLabel, languageCode), translatedText);
        log.info("Saved i18n translation: label='{}', lang='{}', text='{}'", defaultLabel, languageCode, translatedText);
        return saved;
    }

    @Transactional
    public void deleteTranslation(Long id) {
        repository.findById(id).ifPresent(t -> {
            cache.remove(cacheKey(t.getDefaultLabel(), t.getLanguageCode()));
            repository.delete(t);
            log.info("Deleted i18n translation: id={}, label='{}', lang='{}'", id, t.getDefaultLabel(), t.getLanguageCode());
        });
    }

    public List<I18nTranslation> getAllTranslations() {
        return repository.findAllByOrderByDefaultLabelAscLanguageCodeAsc();
    }

    public List<I18nTranslation> getTranslationsByLanguage(String languageCode) {
        return repository.findByLanguageCode(languageCode);
    }

    public void clearCache() {
        cache.clear();
        loadCache();
        log.info("I18n cache cleared and reloaded");
    }

    /**
     * Returns true if the given translated text is still an auto-generated placeholder.
     */
    public boolean isPlaceholder(String translatedText) {
        return translatedText != null && translatedText.startsWith(UNTRANSLATED_PREFIX);
    }

    /**
     * Returns all translations that are still placeholders (starting with "X_").
     */
    public List<I18nTranslation> getUntranslatedEntries() {
        return getAllTranslations().stream()
                .filter(t -> isPlaceholder(t.getTranslatedText()))
                .toList();
    }

    /**
     * Returns all translations for a given language that are still placeholders.
     */
    public List<I18nTranslation> getUntranslatedEntries(String languageCode) {
        return getTranslationsByLanguage(languageCode).stream()
                .filter(t -> isPlaceholder(t.getTranslatedText()))
                .toList();
    }

    private String cacheKey(String defaultLabel, String languageCode) {
        return defaultLabel + "::" + languageCode;
    }
}
