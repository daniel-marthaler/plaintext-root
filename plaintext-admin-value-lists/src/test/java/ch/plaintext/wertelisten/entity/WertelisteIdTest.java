/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WertelisteIdTest {

    @Test
    void noArgsConstructorCreatesEmptyId() {
        WertelisteId id = new WertelisteId();
        assertThat(id.getKey()).isNull();
        assertThat(id.getMandat()).isNull();
    }

    @Test
    void allArgsConstructorSetsFields() {
        WertelisteId id = new WertelisteId("farben", "mandatA");
        assertThat(id.getKey()).isEqualTo("farben");
        assertThat(id.getMandat()).isEqualTo("mandatA");
    }

    @Test
    void equalsAndHashCodeWorkForSameValues() {
        WertelisteId id1 = new WertelisteId("farben", "mandatA");
        WertelisteId id2 = new WertelisteId("farben", "mandatA");

        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    void equalsReturnsFalseForDifferentKeys() {
        WertelisteId id1 = new WertelisteId("farben", "mandatA");
        WertelisteId id2 = new WertelisteId("groessen", "mandatA");

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    void equalsReturnsFalseForDifferentMandat() {
        WertelisteId id1 = new WertelisteId("farben", "mandatA");
        WertelisteId id2 = new WertelisteId("farben", "mandatB");

        assertThat(id1).isNotEqualTo(id2);
    }
}
