/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.service;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.wertelisten.entity.Werteliste;
import ch.plaintext.wertelisten.entity.WertelisteEntry;
import ch.plaintext.wertelisten.repository.WertelisteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WertelistenServiceImplTest {

    @Mock
    private WertelisteRepository repository;

    @Mock
    private PlaintextSecurity security;

    @InjectMocks
    private WertelistenServiceImpl service;

    @Test
    void getWerteReturnsValuesForExistingList() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat("mandatA");
        wl.setEntries(new ArrayList<>());
        wl.addEntry(new WertelisteEntry("Rot", 0));
        wl.addEntry(new WertelisteEntry("Blau", 1));

        when(repository.findByKeyAndMandat("farben", "mandatA")).thenReturn(Optional.of(wl));

        List<String> result = service.getWerte("farben", "mandatA");
        assertThat(result).containsExactly("Rot", "Blau");
    }

    @Test
    void getWerteReturnsEmptyListWhenNotFound() {
        when(repository.findByKeyAndMandat("missing", "mandatA")).thenReturn(Optional.empty());

        List<String> result = service.getWerte("missing", "mandatA");
        assertThat(result).isEmpty();
    }

    @Test
    void getWerteReturnsEmptyListForNullKey() {
        List<String> result = service.getWerte(null, "mandatA");
        assertThat(result).isEmpty();
    }

    @Test
    void getWerteReturnsEmptyListForEmptyKey() {
        List<String> result = service.getWerte("  ", "mandatA");
        assertThat(result).isEmpty();
    }

    @Test
    void getWerteReturnsEmptyListForNullMandat() {
        List<String> result = service.getWerte("farben", null);
        assertThat(result).isEmpty();
    }

    @Test
    void getWerteWithCurrentMandatDelegatesToOverload() {
        when(security.getMandat()).thenReturn("mandatA");
        when(repository.findByKeyAndMandat("farben", "mandatA")).thenReturn(Optional.empty());

        List<String> result = service.getWerte("farben");
        assertThat(result).isEmpty();
        verify(repository).findByKeyAndMandat("farben", "mandatA");
    }

    @Test
    void getWerteWithInvalidMandatReturnsEmptyList() {
        when(security.getMandat()).thenReturn("NO_AUTH");

        List<String> result = service.getWerte("farben");
        assertThat(result).isEmpty();
    }

    @Test
    void getAllKeysReturnsKeys() {
        when(repository.findAllKeysByMandat("mandatA")).thenReturn(List.of("farben", "groessen"));

        List<String> result = service.getAllKeys("mandatA");
        assertThat(result).containsExactly("farben", "groessen");
    }

    @Test
    void getAllKeysReturnsEmptyForNullMandat() {
        List<String> result = service.getAllKeys((String) null);
        assertThat(result).isEmpty();
    }

    @Test
    void getAllKeysWithCurrentMandat() {
        when(security.getMandat()).thenReturn("mandatA");
        when(repository.findAllKeysByMandat("mandatA")).thenReturn(List.of("key1"));

        List<String> result = service.getAllKeys();
        assertThat(result).containsExactly("key1");
    }

    @Test
    void saveWertelisteCreatesNewEntry() {
        when(repository.findByKeyAndMandat("farben", "mandatA")).thenReturn(Optional.empty());
        when(repository.save(any(Werteliste.class))).thenAnswer(inv -> inv.getArgument(0));

        service.saveWerteliste("farben", "mandatA", List.of("Rot", "Blau"));

        ArgumentCaptor<Werteliste> captor = ArgumentCaptor.forClass(Werteliste.class);
        verify(repository).save(captor.capture());

        Werteliste saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo("farben");
        assertThat(saved.getMandat()).isEqualTo("mandatA");
        assertThat(saved.getEntries()).hasSize(2);
        assertThat(saved.getEntries().get(0).getValue()).isEqualTo("Rot");
        assertThat(saved.getEntries().get(0).getSortOrder()).isZero();
        assertThat(saved.getEntries().get(1).getValue()).isEqualTo("Blau");
        assertThat(saved.getEntries().get(1).getSortOrder()).isEqualTo(1);
    }

    @Test
    void saveWertelisteUpdatesExistingEntry() {
        Werteliste existing = new Werteliste();
        existing.setKey("farben");
        existing.setMandat("mandatA");
        existing.setEntries(new ArrayList<>());
        existing.addEntry(new WertelisteEntry("Alt", 0));

        when(repository.findByKeyAndMandat("farben", "mandatA")).thenReturn(Optional.of(existing));
        when(repository.save(any(Werteliste.class))).thenAnswer(inv -> inv.getArgument(0));

        service.saveWerteliste("farben", "mandatA", List.of("Neu"));

        ArgumentCaptor<Werteliste> captor = ArgumentCaptor.forClass(Werteliste.class);
        verify(repository).save(captor.capture());

        Werteliste saved = captor.getValue();
        assertThat(saved.getEntries()).hasSize(1);
        assertThat(saved.getEntries().get(0).getValue()).isEqualTo("Neu");
    }

    @Test
    void saveWertelisteSkipsBlankValues() {
        when(repository.findByKeyAndMandat("farben", "mandatA")).thenReturn(Optional.empty());
        when(repository.save(any(Werteliste.class))).thenAnswer(inv -> inv.getArgument(0));

        service.saveWerteliste("farben", "mandatA", List.of("Rot", "", "  ", "Blau"));

        ArgumentCaptor<Werteliste> captor = ArgumentCaptor.forClass(Werteliste.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEntries()).hasSize(2);
    }

    @Test
    void saveWertelisteHandlesNullValues() {
        when(repository.findByKeyAndMandat("farben", "mandatA")).thenReturn(Optional.empty());
        when(repository.save(any(Werteliste.class))).thenAnswer(inv -> inv.getArgument(0));

        service.saveWerteliste("farben", "mandatA", null);

        verify(repository).save(any(Werteliste.class));
    }

    @Test
    void saveWertelisteThrowsForNullKey() {
        assertThatThrownBy(() -> service.saveWerteliste(null, "mandatA", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Key");
    }

    @Test
    void saveWertelisteThrowsForEmptyKey() {
        assertThatThrownBy(() -> service.saveWerteliste("  ", "mandatA", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveWertelisteThrowsForNullMandat() {
        assertThatThrownBy(() -> service.saveWerteliste("farben", null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mandat");
    }

    @Test
    void deleteWertelisteCallsRepository() {
        service.deleteWerteliste("farben", "mandatA");
        verify(repository).deleteByKeyAndMandat("farben", "mandatA");
    }

    @Test
    void deleteWertelisteThrowsForNullKey() {
        assertThatThrownBy(() -> service.deleteWerteliste(null, "mandatA"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteWertelisteThrowsForNullMandat() {
        assertThatThrownBy(() -> service.deleteWerteliste("farben", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void existsReturnsTrueWhenFound() {
        when(repository.existsByKeyAndMandat("farben", "mandatA")).thenReturn(true);

        assertThat(service.exists("farben", "mandatA")).isTrue();
    }

    @Test
    void existsReturnsFalseWhenNotFound() {
        when(repository.existsByKeyAndMandat("missing", "mandatA")).thenReturn(false);

        assertThat(service.exists("missing", "mandatA")).isFalse();
    }

    @Test
    void existsReturnsFalseForNullKey() {
        assertThat(service.exists(null, "mandatA")).isFalse();
    }

    @Test
    void existsReturnsFalseForNullMandat() {
        assertThat(service.exists("farben", null)).isFalse();
    }

    @Test
    void getAllWertelistenReturnsListForMandat() {
        Werteliste wl = new Werteliste();
        when(repository.findByMandat("mandatA")).thenReturn(List.of(wl));

        List<Werteliste> result = service.getAllWertelisten("mandatA");
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllWertelistenReturnsEmptyForNullMandat() {
        List<Werteliste> result = service.getAllWertelisten(null);
        assertThat(result).isEmpty();
    }

    @Test
    void getAllWertelistenForAllMandateDelegatesToFindAll() {
        when(repository.findAll()).thenReturn(List.of(new Werteliste()));

        List<Werteliste> result = service.getAllWertelistenForAllMandate();
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllWertelistenForCurrentUserUsesSecurityMandat() {
        when(security.getMandat()).thenReturn("mandatA");
        when(repository.findByMandat("mandatA")).thenReturn(List.of(new Werteliste()));

        List<Werteliste> result = service.getAllWertelistenForCurrentUser();
        assertThat(result).hasSize(1);
    }

    @Test
    void getAllWertelistenForCurrentUserReturnsEmptyForInvalidMandat() {
        when(security.getMandat()).thenReturn("NO_USER");

        List<Werteliste> result = service.getAllWertelistenForCurrentUser();
        assertThat(result).isEmpty();
    }
}
