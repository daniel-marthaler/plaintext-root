/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.rechnungen;

/**
 * Options for customized invoice PDF generation.
 * Used by the Auszahlungen module to generate invoices with custom sender, recipient, IBAN and attachment.
 */
public record InvoicePDFOptions(
        String absenderName,
        String absenderStrasse,
        String absenderPlz,
        String absenderOrt,
        String empfaengerName,
        String empfaengerStrasse,
        String empfaengerPlz,
        String empfaengerOrt,
        String creditorIban,
        String creditorName,
        byte[] belegImage,
        String belegContentType,
        boolean ohneMwst,
        String logoUrl
) {
    public InvoicePDFOptions {
        if (absenderName == null || absenderName.isBlank()) {
            throw new IllegalArgumentException("absenderName is required");
        }
    }
}
