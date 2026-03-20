/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WertelisteEntryTest {

    @Test
    void noArgsConstructorCreatesEmptyEntry() {
        WertelisteEntry entry = new WertelisteEntry();
        assertThat(entry.getId()).isNull();
        assertThat(entry.getValue()).isNull();
        assertThat(entry.getSortOrder()).isNull();
        assertThat(entry.getWerteliste()).isNull();
    }

    @Test
    void twoArgConstructorSetsValueAndOrder() {
        WertelisteEntry entry = new WertelisteEntry("Rot", 0);

        assertThat(entry.getValue()).isEqualTo("Rot");
        assertThat(entry.getSortOrder()).isZero();
        assertThat(entry.getId()).isNull();
        assertThat(entry.getWerteliste()).isNull();
    }

    @Test
    void allArgsConstructorSetsAllFields() {
        Werteliste wl = new Werteliste();
        WertelisteEntry entry = new WertelisteEntry(1L, "Blau", 2, wl);

        assertThat(entry.getId()).isEqualTo(1L);
        assertThat(entry.getValue()).isEqualTo("Blau");
        assertThat(entry.getSortOrder()).isEqualTo(2);
        assertThat(entry.getWerteliste()).isSameAs(wl);
    }

    @Test
    void settersAndGettersWork() {
        WertelisteEntry entry = new WertelisteEntry();
        entry.setId(5L);
        entry.setValue("Gruen");
        entry.setSortOrder(3);

        assertThat(entry.getId()).isEqualTo(5L);
        assertThat(entry.getValue()).isEqualTo("Gruen");
        assertThat(entry.getSortOrder()).isEqualTo(3);
    }

    @Test
    void equalsAndHashCodeWork() {
        WertelisteEntry e1 = new WertelisteEntry(1L, "Rot", 0, null);
        WertelisteEntry e2 = new WertelisteEntry(1L, "Rot", 0, null);

        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }
}
