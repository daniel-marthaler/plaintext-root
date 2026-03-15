package ch.plaintext.anforderungen.web;

import ch.plaintext.anforderungen.entity.Anforderung;
import ch.plaintext.anforderungen.service.AnforderungService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * Backing Bean for Claude Summary display page
 */
@Named
@ViewScoped
@Getter
@Setter
@Slf4j
public class ClaudeSummaryBackingBean implements Serializable {

    private final AnforderungService anforderungService;

    private Long anforderungId;
    private Anforderung anforderung;

    public ClaudeSummaryBackingBean(AnforderungService anforderungService) {
        this.anforderungService = anforderungService;
    }

    /**
     * Called by JSF after viewParam is set
     */
    public void init() {
        log.info("ClaudeSummaryBackingBean.init() called with anforderungId: {}", anforderungId);
        if (anforderungId != null) {
            loadAnforderung();
        } else {
            log.warn("init() called but anforderungId is null - will retry in preRenderView");
        }
    }

    /**
     * Fallback if anforderungId was not set during init()
     */
    @PostConstruct
    public void postConstruct() {
        log.info("ClaudeSummaryBackingBean.postConstruct() called");
    }

    private void loadAnforderung() {
        if (anforderungId != null) {
            try {
                anforderung = anforderungService.findById(anforderungId).orElse(null);
                if (anforderung == null) {
                    log.warn("Anforderung not found: {}", anforderungId);
                } else {
                    String summaryPreview = anforderung.getClaudeSummary() != null
                        ? anforderung.getClaudeSummary().substring(0, Math.min(50, anforderung.getClaudeSummary().length()))
                        : "NULL";
                    log.info("Loaded anforderung {} with summary exists: {}, length: {}, preview: {}",
                            anforderungId,
                            anforderung.getClaudeSummary() != null ? "YES" : "NO",
                            anforderung.getClaudeSummary() != null ? anforderung.getClaudeSummary().length() : 0,
                            summaryPreview);
                }
            } catch (Exception e) {
                log.error("Error loading anforderung: {}", anforderungId, e);
            }
        } else {
            log.warn("anforderungId is null in loadAnforderung()");
        }
    }

    /**
     * Get the markdown HTML content (already rendered if available)
     */
    public String getMarkdownHtml() {
        log.debug("getMarkdownHtml() called: anforderung={}, summary={}",
                anforderung != null ? anforderung.getId() : "null",
                anforderung != null && anforderung.getClaudeSummary() != null ? "exists (length=" + anforderung.getClaudeSummary().length() + ")" : "null");
        if (anforderung == null || anforderung.getClaudeSummary() == null) {
            return null;
        }
        return anforderung.getClaudeSummary();
    }

    /**
     * Get markdown content as JSON-escaped string for JavaScript
     */
    public String getMarkdownContentJson() {
        if (anforderung == null || anforderung.getClaudeSummary() == null) {
            return "\"\"";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(anforderung.getClaudeSummary());
        } catch (Exception e) {
            log.error("Error converting markdown to JSON", e);
            return "\"\"";
        }
    }

    /**
     * Check if summary exists
     */
    public boolean hasSummary() {
        boolean result = anforderung != null
            && anforderung.getClaudeSummary() != null
            && !anforderung.getClaudeSummary().trim().isEmpty();
        log.info("hasSummary() called: anforderung={}, claudeSummary={}, isEmpty={}, result={}",
                anforderung != null ? anforderung.getId() : "null",
                anforderung != null && anforderung.getClaudeSummary() != null ? "exists (length=" + anforderung.getClaudeSummary().length() + ")" : "null",
                anforderung != null && anforderung.getClaudeSummary() != null ? anforderung.getClaudeSummary().trim().isEmpty() : "N/A",
                result);
        return result;
    }
}
