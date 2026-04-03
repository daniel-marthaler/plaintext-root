/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.oidc.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.oidc.entity.OidcConfig;
import ch.plaintext.oidc.service.OidcConfigService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Scope("view")
@Component
@Data
public class OidcConfigBackingBean implements Serializable {
    private static final long serialVersionUID = 1L;

    @Autowired
    private OidcConfigService oidcConfigService;

    @Autowired
    private PlaintextSecurity plaintextSecurity;

    private List<OidcConfig> configs = new ArrayList<>();
    private OidcConfig selected;
    private String testResult;

    @PostConstruct
    public void init() {
        if (!isRoot()) {
            return;
        }
        loadData();
    }

    public void loadData() {
        configs = oidcConfigService.findAll();
        if (!configs.isEmpty() && selected == null) {
            selected = configs.getFirst();
        }
    }

    public boolean isRoot() {
        return plaintextSecurity != null && plaintextSecurity.ifGranted("ROLE_root");
    }

    public void checkAccess() {
        if (!isRoot()) {
            log.warn("SECURITY: User attempted to access OIDC config without ROLE_root");
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("access-denied.xhtml");
            } catch (Exception e) {
                log.error("Error redirecting to access denied page", e);
            }
        }
    }

    public void newConfig() {
        selected = new OidcConfig();
    }

    public void select(OidcConfig config) {
        this.selected = config;
        this.testResult = null;
    }

    public void save() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (selected.getIssuerUrl() == null || selected.getIssuerUrl().isBlank()) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Issuer-URL darf nicht leer sein."));
            context.validationFailed();
            return;
        }

        if (selected.getClientId() == null || selected.getClientId().isBlank()) {
            context.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", "Client-ID darf nicht leer sein."));
            context.validationFailed();
            return;
        }

        selected = oidcConfigService.save(selected);
        loadData();
        context.addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "OIDC-Konfiguration gespeichert."));
    }

    public void delete() {
        if (selected != null) {
            oidcConfigService.delete(selected);
            selected = null;
            loadData();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", "OIDC-Konfiguration gelöscht."));
        }
    }

    public void testConnection() {
        if (selected == null) {
            testResult = "Keine Konfiguration ausgewählt";
            return;
        }
        testResult = oidcConfigService.testConnection(selected);

        FacesMessage.Severity severity = "OK".equals(testResult)
                ? FacesMessage.SEVERITY_INFO
                : FacesMessage.SEVERITY_ERROR;
        String summary = "OK".equals(testResult) ? "Verbindung erfolgreich" : "Verbindung fehlgeschlagen";
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severity, summary, testResult));
    }

    public List<String> getScopesList() {
        if (selected == null || selected.getScopes() == null || selected.getScopes().isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(selected.getScopes().split(",")));
    }

    public void setScopesList(List<String> scopes) {
        if (selected != null) {
            selected.setScopes(scopes != null ? String.join(",", scopes) : "");
        }
    }
}
