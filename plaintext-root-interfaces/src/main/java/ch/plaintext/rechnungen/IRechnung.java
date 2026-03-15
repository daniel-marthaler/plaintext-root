package ch.plaintext.rechnungen;

import java.util.Date;

/**
 * Interface für eine Rechnung
 */
public interface IRechnung {

    /**
     * Gibt die ID der Rechnung zurück
     */
    Long getId();

    /**
     * Gibt die Rechnungsnummer zurück (z.B. "R-2026-001")
     */
    String getRechnungsnummer();

    /**
     * Gibt die Kontakt-ID zurück
     */
    Long getKontaktId();

    /**
     * Gibt die Betreffzeile zurück
     */
    String getBetreff();

    /**
     * Gibt den Preis pro Stunde zurück
     */
    double getPreisProStunde();

    /**
     * Gibt die Gesamtstunden zurück
     */
    double getGesamtStunden();

    /**
     * Gibt den Gesamtbetrag zurück
     */
    double getGesamtbetrag();

    /**
     * Gibt das Rechnungsdatum zurück
     */
    Date getRechnungsdatum();

    /**
     * Gibt den Zeitraum zurück (z.B. "Januar 2026")
     */
    String getZeitraum();
}
