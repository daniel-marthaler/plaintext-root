/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
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
