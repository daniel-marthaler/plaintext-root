/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class WertelisteTest {

    @Test
    void noArgsConstructorCreatesEmptyEntity() {
        Werteliste wl = new Werteliste();
        assertThat(wl.getKey()).isNull();
        assertThat(wl.getMandat()).isNull();
        assertThat(wl.getEntries()).isEmpty();
    }

    @Test
    void settersAndGettersWork() {
        Werteliste wl = new Werteliste();
        LocalDateTime now = LocalDateTime.now();

        wl.setKey("farben");
        wl.setMandat("mandatA");
        wl.setCreatedDate(now);
        wl.setCreatedBy("admin");
        wl.setLastModifiedDate(now);
        wl.setLastModifiedBy("editor");

        assertThat(wl.getKey()).isEqualTo("farben");
        assertThat(wl.getMandat()).isEqualTo("mandatA");
        assertThat(wl.getCreatedDate()).isEqualTo(now);
        assertThat(wl.getCreatedBy()).isEqualTo("admin");
        assertThat(wl.getLastModifiedDate()).isEqualTo(now);
        assertThat(wl.getLastModifiedBy()).isEqualTo("editor");
    }

    @Test
    void addEntrySetsBackReferenceAndAddsToList() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry entry = new WertelisteEntry("Rot", 0);

        wl.addEntry(entry);

        assertThat(wl.getEntries()).hasSize(1);
        assertThat(wl.getEntries().get(0).getValue()).isEqualTo("Rot");
        assertThat(entry.getWerteliste()).isSameAs(wl);
    }

    @Test
    void removeEntryRemovesAndClearsBackReference() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry entry = new WertelisteEntry("Blau", 0);
        wl.addEntry(entry);

        wl.removeEntry(entry);

        assertThat(wl.getEntries()).isEmpty();
        assertThat(entry.getWerteliste()).isNull();
    }

    @Test
    void addMultipleEntries() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());

        wl.addEntry(new WertelisteEntry("Rot", 0));
        wl.addEntry(new WertelisteEntry("Blau", 1));
        wl.addEntry(new WertelisteEntry("Gruen", 2));

        assertThat(wl.getEntries()).hasSize(3);
    }
}
