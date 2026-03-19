/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.filelist.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.filelist.entity.FileMetadata;
import ch.plaintext.filelist.service.FilelistService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.model.file.UploadedFile;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

@Named
@ViewScoped
@Getter
@Setter
@Slf4j
public class FilelistBackingBean implements Serializable {

    private final FilelistService service;
    private final PlaintextSecurity security;

    private List<FileMetadata> files;
    private FileMetadata selected;
    private UploadedFile uploadedFile;
    private String uploadCategory;
    private String uploadStorageBackend = "VFS";
    private boolean admin;

    public FilelistBackingBean(FilelistService service, PlaintextSecurity security) {
        this.service = service;
        this.security = security;
    }

    @PostConstruct
    public void init() {
        admin = security.ifGranted("ROLE_ADMIN") || security.ifGranted("ROLE_ROOT");
        loadData();
    }

    public void checkAccess() {
        if (!admin) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("/index.xhtml");
            } catch (Exception e) {
                log.error("Redirect failed", e);
            }
        }
    }

    private void loadData() {
        try {
            files = service.getAllFilesForCurrentUser();
        } catch (Exception e) {
            log.error("Error loading files", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Daten konnten nicht geladen werden");
        }
    }

    public void select() {
        // Selected in UI
    }

    public void clearSelection() {
        selected = null;
    }

    public void upload() {
        if (uploadedFile == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Datei ausgewählt");
            return;
        }

        try {
            InputStream inputStream = uploadedFile.getInputStream();
            service.uploadFile(inputStream, uploadedFile.getFileName(),
                             uploadedFile.getContentType(), uploadedFile.getSize(),
                             uploadCategory, uploadStorageBackend);
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Datei hochgeladen");
            uploadedFile = null;
            uploadCategory = null;
            loadData();
        } catch (Exception e) {
            log.error("Error uploading file", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Hochladen fehlgeschlagen");
        }
    }

    public void download() {
        if (selected == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Datei ausgewählt");
            return;
        }

        try {
            InputStream inputStream = service.downloadFile(selected.getId());
            FacesContext fc = FacesContext.getCurrentInstance();
            fc.getExternalContext().responseReset();
            fc.getExternalContext().setResponseContentType(selected.getContentType());
            fc.getExternalContext().setResponseHeader("Content-Disposition",
                    "attachment; filename=\"" + selected.getFilename() + "\"");

            inputStream.transferTo(fc.getExternalContext().getResponseOutputStream());
            fc.responseComplete();
        } catch (Exception e) {
            log.error("Error downloading file", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Download fehlgeschlagen");
        }
    }

    public void delete() {
        if (selected == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Datei ausgewählt");
            return;
        }

        try {
            service.deleteFile(selected.getId());
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Datei gelöscht");
            selected = null;
            loadData();
        } catch (Exception e) {
            log.error("Error deleting file", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Löschen fehlgeschlagen");
        }
    }

    public void updateMetadata() {
        if (selected == null) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Datei ausgewählt");
            return;
        }

        try {
            service.updateMetadata(selected);
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Metadaten aktualisiert");
            loadData();
        } catch (Exception e) {
            log.error("Error updating metadata", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Aktualisierung fehlgeschlagen");
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    public List<String> getAvailableStorageBackends() {
        return service.getAvailableStorageBackends();
    }

    public List<String> getAllCategories() {
        return service.getAllCategories(security.getMandat());
    }

    public String getTotalStorageUsedFormatted() {
        Long bytes = service.getTotalStorageUsed(security.getMandat());
        return formatBytes(bytes);
    }

    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
