/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.apitoken;

import ch.plaintext.PlaintextSecurity;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Root Backing Bean for API Token management.
 * Shows all tokens across all mandats. Only accessible by ROOT users.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Named("rootApiTokenBean")
@ViewScoped
@Slf4j
public class RootApiTokenBackingBean implements Serializable {

    @Autowired
    private transient ApiTokenService apiTokenService;

    @Autowired
    private transient PlaintextSecurity security;

    @Getter
    private List<RootTokenDisplay> tokens = new ArrayList<>();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @PostConstruct
    public void init() {
        if (!security.ifGranted("ROLE_ROOT")) {
            return;
        }
        loadTokens();
    }

    public void checkAccess() {
        if (!security.ifGranted("ROLE_ROOT")) {
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect("/index.xhtml");
            } catch (Exception e) {
                log.error("Redirect failed", e);
            }
        }
    }

    private void loadTokens() {
        List<ApiToken> apiTokens = apiTokenService.getAllTokensAllMandats();
        this.tokens = new ArrayList<>();

        for (ApiToken t : apiTokens) {
            RootTokenDisplay display = new RootTokenDisplay();
            display.setId(t.getId());
            display.setMandat(t.getMandat() != null ? t.getMandat() : "-");
            display.setTokenName(t.getTokenName() != null ? t.getTokenName() : "Unbenannt");
            display.setUserId(t.getUserId());
            display.setUserEmail(t.getUserEmail() != null ? t.getUserEmail() : "-");
            display.setCreatedAt(formatDate(t.getCreatedAt()));
            display.setLastUsedAt(t.getLastUsedAt() != null ? formatDate(t.getLastUsedAt()) : "Noch nie");
            display.setExpiresAt(formatDate(t.getExpiresAt()));
            display.setExpiresSoon(apiTokenService.willExpireSoon(t, Duration.ofDays(7)));
            display.setExpired(t.isExpired());
            display.setUseCount(t.getUseCount());

            if (t.getExpiresAt() != null) {
                long daysUntilExpiry = Duration.between(LocalDateTime.now(), t.getExpiresAt()).toDays();
                display.setDaysUntilExpiry(daysUntilExpiry);
            }

            this.tokens.add(display);
        }
    }

    private String formatDate(LocalDateTime date) {
        return date != null ? date.format(DATE_FORMAT) : "-";
    }

    public void invalidateToken(Long tokenId) {
        if (!security.ifGranted("ROLE_ROOT")) {
            addError("Kein Root-Zugriff.");
            return;
        }

        apiTokenService.invalidateTokenByRoot(tokenId);
        addInfo("Token wurde invalidiert.");
        loadTokens();
    }

    public boolean hasTokens() {
        return !tokens.isEmpty();
    }

    private void addInfo(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Erfolg", message));
    }

    private void addError(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Fehler", message));
    }

    @Getter
    @Setter
    public static class RootTokenDisplay implements Serializable {
        private Long id;
        private String mandat;
        private String tokenName;
        private Long userId;
        private String userEmail;
        private String createdAt;
        private String lastUsedAt;
        private String expiresAt;
        private boolean expiresSoon;
        private boolean expired;
        private long daysUntilExpiry;
        private long useCount;
    }
}
