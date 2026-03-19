/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.wertelisten.entity.Werteliste;
import ch.plaintext.wertelisten.entity.WertelisteEntry;
import ch.plaintext.wertelisten.service.WertelistenServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
@Getter
@Setter
@Slf4j
public class WertelistenBackingBean implements Serializable {

    private final WertelistenServiceImpl service;
    private final PlaintextSecurity security;

    private List<Werteliste> wertelisten;
    private Werteliste selected;
    private String newKey;
    private String newValue;
    private boolean root;

    public WertelistenBackingBean(WertelistenServiceImpl service, PlaintextSecurity security) {
        this.service = service;
        this.security = security;
    }

    @PostConstruct
    public void init() {
        root = security.ifGranted("ROLE_ROOT");
        loadData();
    }

    public void checkAccess() {
        if (!security.ifGranted("ROLE_ADMIN") && !root) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("/index.xhtml");
            } catch (Exception e) {
                log.error("Redirect failed", e);
            }
        }
    }

    private void loadData() {
        try {
            if (root) {
                // Root sees all wertelisten from all mandate
                wertelisten = service.getAllWertelistenForAllMandate();
            } else {
                // Admin sees only own mandat
                wertelisten = service.getAllWertelistenForCurrentUser();
            }
        } catch (Exception e) {
            log.error("Error loading wertelisten", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Daten konnten nicht geladen werden");
        }
    }

    public void select() {
        // Selected in UI
    }

    public void clearSelection() {
        selected = null;
    }

    public void newWerteliste() {
        selected = new Werteliste();
        selected.setKey("");
        selected.setMandat(root ? "" : security.getMandat());
        selected.setEntries(new ArrayList<>());
    }

    public void save() {
        if (selected == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Werteliste ausgewählt");
            return;
        }

        if (selected.getKey() == null || selected.getKey().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Schlüssel ist erforderlich");
            return;
        }

        if (selected.getMandat() == null || selected.getMandat().trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Mandat ist erforderlich");
            return;
        }

        try {
            List<String> values = selected.getEntries().stream()
                    .map(WertelisteEntry::getValue)
                    .collect(Collectors.toList());
            service.saveWerteliste(selected.getKey(), selected.getMandat(), values);
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Werteliste gespeichert");
            loadData();
        } catch (Exception e) {
            log.error("Error saving werteliste", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Speichern fehlgeschlagen");
        }
    }

    public void delete() {
        if (selected == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Werteliste ausgewählt");
            return;
        }

        try {
            service.deleteWerteliste(selected.getKey(), selected.getMandat());
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Werteliste gelöscht");
            selected = null;
            loadData();
        } catch (Exception e) {
            log.error("Error deleting werteliste", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Löschen fehlgeschlagen");
        }
    }

    public void addEntry() {
        if (selected == null) {
            return;
        }
        if (newValue == null || newValue.trim().isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Wert ist erforderlich");
            return;
        }

        int maxOrder = selected.getEntries().stream()
                .mapToInt(WertelisteEntry::getSortOrder)
                .max()
                .orElse(-1);

        WertelisteEntry entry = new WertelisteEntry(newValue.trim(), maxOrder + 1);
        selected.addEntry(entry);
        newValue = "";
    }

    public void removeEntry(WertelisteEntry entry) {
        if (selected != null) {
            selected.removeEntry(entry);
        }
    }

    public void moveUp(WertelisteEntry entry) {
        if (selected == null) return;
        List<WertelisteEntry> entries = selected.getEntries();
        int index = entries.indexOf(entry);
        if (index > 0) {
            entries.remove(index);
            entries.add(index - 1, entry);
            reorderEntries();
        }
    }

    public void moveDown(WertelisteEntry entry) {
        if (selected == null) return;
        List<WertelisteEntry> entries = selected.getEntries();
        int index = entries.indexOf(entry);
        if (index < entries.size() - 1) {
            entries.remove(index);
            entries.add(index + 1, entry);
            reorderEntries();
        }
    }

    private void reorderEntries() {
        if (selected == null) return;
        int order = 0;
        for (WertelisteEntry entry : selected.getEntries()) {
            entry.setSortOrder(order++);
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    public List<String> getAllMandate() {
        return new ArrayList<>(security.getAllMandate());
    }
}
