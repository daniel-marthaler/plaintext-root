/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.settings.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.settings.entity.BrandingLogo;
import ch.plaintext.settings.service.BrandingService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Base64;

@Named
@ViewScoped
@Getter
@Setter
@Slf4j
public class BrandingBackingBean implements Serializable {

    private final transient BrandingService brandingService;
    private final PlaintextSecurity security;
    private final transient ApplicationContext applicationContext;

    private String footerText;
    private boolean showVersion;
    private boolean showRootVersion;

    private Integer lightLogoWidth = 180;
    private Integer lightLogoHeight = 40;
    private Integer darkLogoWidth = 180;
    private Integer darkLogoHeight = 40;

    private boolean hasLightLogo;
    private boolean hasDarkLogo;
    private String lightLogoPreview;
    private String darkLogoPreview;

    private boolean root;

    public BrandingBackingBean(BrandingService brandingService, PlaintextSecurity security,
                              ApplicationContext applicationContext) {
        this.brandingService = brandingService;
        this.security = security;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init() {
        root = security.ifGranted("ROLE_ROOT");
        if (root) {
            loadData();
        }
    }

    public void checkAccess() {
        if (!root) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("/index.xhtml");
            } catch (Exception e) {
                log.error("Redirect failed", e);
            }
        }
    }

    private void loadData() {
        String mandat = security.getMandat();

        footerText = brandingService.getFooterText(mandat);
        showVersion = brandingService.isShowVersion(mandat);
        showRootVersion = brandingService.isShowRootVersion(mandat);

        brandingService.getLogo(mandat, "light").ifPresent(logo -> {
            hasLightLogo = true;
            lightLogoWidth = logo.getLogoWidth();
            lightLogoHeight = logo.getLogoHeight();
            lightLogoPreview = buildDataUri(logo);
        });

        brandingService.getLogo(mandat, "dark").ifPresent(logo -> {
            hasDarkLogo = true;
            darkLogoWidth = logo.getLogoWidth();
            darkLogoHeight = logo.getLogoHeight();
            darkLogoPreview = buildDataUri(logo);
        });
    }

    public void handleLightLogoUpload(FileUploadEvent event) {
        handleLogoUpload(event, "light");
    }

    public void handleDarkLogoUpload(FileUploadEvent event) {
        handleLogoUpload(event, "dark");
    }

    private void handleLogoUpload(FileUploadEvent event, String theme) {
        UploadedFile file = event.getFile();
        if (file == null || file.getContent() == null || file.getContent().length == 0) {
            addMessage(FacesMessage.SEVERITY_WARN, "Warnung", "Keine Datei ausgewählt");
            return;
        }

        try {
            Integer width = "light".equals(theme) ? lightLogoWidth : darkLogoWidth;
            Integer height = "light".equals(theme) ? lightLogoHeight : darkLogoHeight;

            brandingService.saveLogo(
                    security.getMandat(), theme,
                    file.getContent(), file.getContentType(), file.getFileName(),
                    width, height
            );

            // Update preview
            if ("light".equals(theme)) {
                hasLightLogo = true;
                lightLogoPreview = "data:" + file.getContentType() + ";base64,"
                        + Base64.getEncoder().encodeToString(file.getContent());
            } else {
                hasDarkLogo = true;
                darkLogoPreview = "data:" + file.getContentType() + ";base64,"
                        + Base64.getEncoder().encodeToString(file.getContent());
            }

            refreshBrandingBean();
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg",
                    (theme.equals("light") ? "Light" : "Dark") + "-Logo hochgeladen");
        } catch (IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", e.getMessage());
        } catch (Exception e) {
            log.error("Error uploading logo", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Upload fehlgeschlagen");
        }
    }

    public void deleteLightLogo() {
        try {
            brandingService.deleteLogo(security.getMandat(), "light");
            hasLightLogo = false;
            lightLogoPreview = null;
            lightLogoWidth = 180;
            lightLogoHeight = 40;
            refreshBrandingBean();
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Light-Logo gelöscht");
        } catch (Exception e) {
            log.error("Error deleting light logo", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Löschen fehlgeschlagen");
        }
    }

    public void deleteDarkLogo() {
        try {
            brandingService.deleteLogo(security.getMandat(), "dark");
            hasDarkLogo = false;
            darkLogoPreview = null;
            darkLogoWidth = 180;
            darkLogoHeight = 40;
            refreshBrandingBean();
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Dark-Logo gelöscht");
        } catch (Exception e) {
            log.error("Error deleting dark logo", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Löschen fehlgeschlagen");
        }
    }

    public void saveFooterSettings() {
        try {
            brandingService.saveFooterSettings(security.getMandat(),
                    footerText, showVersion, showRootVersion);
            refreshBrandingBean();
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Footer-Einstellungen gespeichert");
        } catch (Exception e) {
            log.error("Error saving footer settings", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Speichern fehlgeschlagen");
        }
    }

    public void saveLightLogoDimensions() {
        saveDimensions("light", lightLogoWidth, lightLogoHeight);
    }

    public void saveDarkLogoDimensions() {
        saveDimensions("dark", darkLogoWidth, darkLogoHeight);
    }

    private void saveDimensions(String theme, Integer width, Integer height) {
        try {
            String mandat = security.getMandat();
            brandingService.getLogo(mandat, theme).ifPresent(logo -> {
                logo.setLogoWidth(width);
                logo.setLogoHeight(height);
                brandingService.updateLogoDimensions(logo);
            });
            refreshBrandingBean();
            addMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "Dimensionen gespeichert");
        } catch (Exception e) {
            log.error("Error saving logo dimensions", e);
            addMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Speichern fehlgeschlagen");
        }
    }

    private String buildDataUri(BrandingLogo logo) {
        return "data:" + logo.getContentType() + ";base64,"
                + logo.getImageData();
    }

    private void refreshBrandingBean() {
        try {
            Object brandingBean = applicationContext.getBean("brandingBean");
            Method refresh = brandingBean.getClass().getMethod("refresh");
            refresh.invoke(brandingBean);
        } catch (Exception e) {
            log.debug("Could not refresh BrandingBean: {}", e.getMessage());
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
