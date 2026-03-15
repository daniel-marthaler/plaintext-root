package ch.plaintext.wertelisten.service;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.wertelisten.IWertelistenService;
import ch.plaintext.wertelisten.entity.Werteliste;
import ch.plaintext.wertelisten.entity.WertelisteEntry;
import ch.plaintext.wertelisten.repository.WertelisteRepository;
import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Named("wertelistenService")
@Slf4j
public class WertelistenServiceImpl implements IWertelistenService {

    private final WertelisteRepository repository;
    private final PlaintextSecurity security;

    public WertelistenServiceImpl(WertelisteRepository repository, PlaintextSecurity security) {
        this.repository = repository;
        this.security = security;
    }

    @Override
    public List<String> getWerte(String key, String mandat) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("getWerte called with null or empty key");
            return new ArrayList<>();
        }
        if (mandat == null || mandat.trim().isEmpty()) {
            log.warn("getWerte called with null or empty mandat");
            return new ArrayList<>();
        }

        try {
            return repository.findByKeyAndMandat(key, mandat)
                    .map(werteliste -> werteliste.getEntries().stream()
                            .map(WertelisteEntry::getValue)
                            .collect(Collectors.toList()))
                    .orElse(new ArrayList<>());
        } catch (Exception e) {
            log.error("Error getting werte for key={}, mandat={}", key, mandat, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getWerte(String key) {
        String mandat = security.getMandat();
        if (mandat == null || "NO_AUTH".equals(mandat) || "NO_USER".equals(mandat) || "ERROR".equals(mandat)) {
            log.warn("Cannot get werte - invalid mandat: {}", mandat);
            return new ArrayList<>();
        }
        return getWerte(key, mandat);
    }

    @Override
    public List<String> getAllKeys(String mandat) {
        if (mandat == null || mandat.trim().isEmpty()) {
            log.warn("getAllKeys called with null or empty mandat");
            return new ArrayList<>();
        }

        try {
            return repository.findAllKeysByMandat(mandat);
        } catch (Exception e) {
            log.error("Error getting all keys for mandat={}", mandat, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<String> getAllKeys() {
        String mandat = security.getMandat();
        if (mandat == null || "NO_AUTH".equals(mandat) || "NO_USER".equals(mandat) || "ERROR".equals(mandat)) {
            log.warn("Cannot get all keys - invalid mandat: {}", mandat);
            return new ArrayList<>();
        }
        return getAllKeys(mandat);
    }

    @Override
    @Transactional
    public void saveWerteliste(String key, String mandat, List<String> values) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (mandat == null || mandat.trim().isEmpty()) {
            throw new IllegalArgumentException("Mandat cannot be null or empty");
        }
        if (values == null) {
            values = new ArrayList<>();
        }

        try {
            Werteliste werteliste = repository.findByKeyAndMandat(key, mandat)
                    .orElse(new Werteliste());

            werteliste.setKey(key);
            werteliste.setMandat(mandat);

            // Clear existing entries
            werteliste.getEntries().clear();

            // Add new entries
            int sortOrder = 0;
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    WertelisteEntry entry = new WertelisteEntry(value.trim(), sortOrder++);
                    werteliste.addEntry(entry);
                }
            }

            repository.save(werteliste);
            log.info("Saved werteliste: key={}, mandat={}, entries={}", key, mandat, values.size());
        } catch (Exception e) {
            log.error("Error saving werteliste: key={}, mandat={}", key, mandat, e);
            throw new RuntimeException("Failed to save werteliste", e);
        }
    }

    @Override
    @Transactional
    public void deleteWerteliste(String key, String mandat) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        if (mandat == null || mandat.trim().isEmpty()) {
            throw new IllegalArgumentException("Mandat cannot be null or empty");
        }

        try {
            repository.deleteByKeyAndMandat(key, mandat);
            log.info("Deleted werteliste: key={}, mandat={}", key, mandat);
        } catch (Exception e) {
            log.error("Error deleting werteliste: key={}, mandat={}", key, mandat, e);
            throw new RuntimeException("Failed to delete werteliste", e);
        }
    }

    @Override
    public boolean exists(String key, String mandat) {
        if (key == null || key.trim().isEmpty() || mandat == null || mandat.trim().isEmpty()) {
            return false;
        }

        try {
            return repository.existsByKeyAndMandat(key, mandat);
        } catch (Exception e) {
            log.error("Error checking existence: key={}, mandat={}", key, mandat, e);
            return false;
        }
    }

    public List<Werteliste> getAllWertelisten(String mandat) {
        if (mandat == null || mandat.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return repository.findByMandat(mandat);
    }

    public List<Werteliste> getAllWertelistenForAllMandate() {
        return repository.findAll();
    }

    public List<Werteliste> getAllWertelistenForCurrentUser() {
        String mandat = security.getMandat();
        if (mandat == null || "NO_AUTH".equals(mandat) || "NO_USER".equals(mandat) || "ERROR".equals(mandat)) {
            return new ArrayList<>();
        }
        return getAllWertelisten(mandat);
    }
}
