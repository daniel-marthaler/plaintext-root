package ch.plaintext.rechnungen;

/**
 * Interface für eine Kontakt-Option im Dropdown
 */
public interface IKontaktOption {

    /**
     * Gibt die ID des Kontakts zurück
     */
    Long getId();

    /**
     * Gibt den Anzeigenamen zurück (z.B. "Max Mustermann - Firma AG")
     */
    String getDisplayName();
}
