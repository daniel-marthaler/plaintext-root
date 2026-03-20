/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.wertelisten.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.wertelisten.entity.Werteliste;
import ch.plaintext.wertelisten.entity.WertelisteEntry;
import ch.plaintext.wertelisten.service.WertelistenServiceImpl;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WertelistenBackingBeanTest {

    @Mock
    private WertelistenServiceImpl service;

    @Mock
    private PlaintextSecurity security;

    @Mock
    private FacesContext facesContext;

    @Mock
    private ExternalContext externalContext;

    private MockedStatic<FacesContext> facesContextStatic;

    private WertelistenBackingBean bean;

    @BeforeEach
    void setUp() {
        facesContextStatic = mockStatic(FacesContext.class);
        facesContextStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
        lenient().when(facesContext.getExternalContext()).thenReturn(externalContext);

        bean = new WertelistenBackingBean(service, security);
    }

    @AfterEach
    void tearDown() {
        facesContextStatic.close();
    }

    // --- init() ---

    @Test
    void initLoadsDataForRoot() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(true);
        when(service.getAllWertelistenForAllMandate()).thenReturn(List.of(new Werteliste()));

        bean.init();

        assertThat(bean.isRoot()).isTrue();
        assertThat(bean.getWertelisten()).hasSize(1);
        verify(service).getAllWertelistenForAllMandate();
    }

    @Test
    void initLoadsDataForNonRoot() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(false);
        when(service.getAllWertelistenForCurrentUser()).thenReturn(List.of(new Werteliste()));

        bean.init();

        assertThat(bean.isRoot()).isFalse();
        assertThat(bean.getWertelisten()).hasSize(1);
        verify(service).getAllWertelistenForCurrentUser();
    }

    @Test
    void initHandlesExceptionDuringLoadData() {
        when(security.ifGranted("ROLE_ROOT")).thenReturn(false);
        when(service.getAllWertelistenForCurrentUser()).thenThrow(new RuntimeException("DB error"));

        bean.init();

        verify(facesContext).addMessage(isNull(), any(FacesMessage.class));
    }

    // --- checkAccess() ---

    @Test
    void checkAccessAllowsAdmin() throws Exception {
        when(security.ifGranted("ROLE_ADMIN")).thenReturn(true);

        bean.checkAccess();

        verify(externalContext, never()).redirect(any());
    }

    @Test
    void checkAccessAllowsRoot() throws Exception {
        when(security.ifGranted("ROLE_ADMIN")).thenReturn(false);
        bean.setRoot(true);

        bean.checkAccess();

        verify(externalContext, never()).redirect(any());
    }

    @Test
    void checkAccessRedirectsNonAdmin() throws Exception {
        when(security.ifGranted("ROLE_ADMIN")).thenReturn(false);
        bean.setRoot(false);

        bean.checkAccess();

        verify(externalContext).redirect("/index.xhtml");
    }

    @Test
    void checkAccessHandlesRedirectException() throws Exception {
        when(security.ifGranted("ROLE_ADMIN")).thenReturn(false);
        bean.setRoot(false);
        doThrow(new RuntimeException("redirect failed")).when(externalContext).redirect(any());

        bean.checkAccess(); // should not throw
    }

    // --- select() / clearSelection() ---

    @Test
    void selectDoesNothing() {
        bean.select(); // no-op, just coverage
    }

    @Test
    void clearSelectionNullifiesSelected() {
        bean.setSelected(new Werteliste());

        bean.clearSelection();

        assertThat(bean.getSelected()).isNull();
    }

    // --- newWerteliste() ---

    @Test
    void newWertelisteCreatesForNonRoot() {
        bean.setRoot(false);
        when(security.getMandat()).thenReturn("mandatA");

        bean.newWerteliste();

        assertThat(bean.getSelected()).isNotNull();
        assertThat(bean.getSelected().getKey()).isEmpty();
        assertThat(bean.getSelected().getMandat()).isEqualTo("mandatA");
        assertThat(bean.getSelected().getEntries()).isEmpty();
    }

    @Test
    void newWertelisteCreatesForRoot() {
        bean.setRoot(true);

        bean.newWerteliste();

        assertThat(bean.getSelected()).isNotNull();
        assertThat(bean.getSelected().getMandat()).isEmpty();
    }

    // --- save() ---

    @Test
    void saveWithNoSelectionShowsWarning() {
        bean.setSelected(null);

        bean.save();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_WARN);
    }

    @Test
    void saveWithEmptyKeyShowsWarning() {
        Werteliste wl = new Werteliste();
        wl.setKey("  ");
        wl.setMandat("mandatA");
        bean.setSelected(wl);

        bean.save();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_WARN);
        assertThat(captor.getValue().getDetail()).contains("Schlüssel");
    }

    @Test
    void saveWithNullKeyShowsWarning() {
        Werteliste wl = new Werteliste();
        wl.setKey(null);
        wl.setMandat("mandatA");
        bean.setSelected(wl);

        bean.save();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_WARN);
    }

    @Test
    void saveWithEmptyMandatShowsWarning() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat("  ");
        bean.setSelected(wl);

        bean.save();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getDetail()).contains("Mandat");
    }

    @Test
    void saveWithNullMandatShowsWarning() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat(null);
        bean.setSelected(wl);

        bean.save();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_WARN);
    }

    @Test
    void saveSuccessfullyCallsService() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat("mandatA");
        wl.setEntries(new ArrayList<>());
        wl.addEntry(new WertelisteEntry("Rot", 0));
        bean.setSelected(wl);
        bean.setRoot(false);
        when(service.getAllWertelistenForCurrentUser()).thenReturn(new ArrayList<>());

        bean.save();

        verify(service).saveWerteliste(eq("farben"), eq("mandatA"), any());
        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
    }

    @Test
    void saveHandlesServiceException() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat("mandatA");
        wl.setEntries(new ArrayList<>());
        bean.setSelected(wl);
        doThrow(new RuntimeException("DB error")).when(service).saveWerteliste(any(), any(), any());

        bean.save();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
    }

    // --- delete() ---

    @Test
    void deleteWithNoSelectionShowsWarning() {
        bean.setSelected(null);

        bean.delete();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_WARN);
    }

    @Test
    void deleteSuccessfullyCallsService() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat("mandatA");
        bean.setSelected(wl);
        bean.setRoot(false);
        when(service.getAllWertelistenForCurrentUser()).thenReturn(new ArrayList<>());

        bean.delete();

        verify(service).deleteWerteliste("farben", "mandatA");
        assertThat(bean.getSelected()).isNull();
    }

    @Test
    void deleteHandlesServiceException() {
        Werteliste wl = new Werteliste();
        wl.setKey("farben");
        wl.setMandat("mandatA");
        bean.setSelected(wl);
        doThrow(new RuntimeException("DB error")).when(service).deleteWerteliste(any(), any());

        bean.delete();

        ArgumentCaptor<FacesMessage> captor = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
    }

    // --- addEntry() ---

    @Test
    void addEntryDoesNothingWhenNoSelection() {
        bean.setSelected(null);
        bean.setNewValue("test");

        bean.addEntry();

        assertThat(bean.getNewValue()).isEqualTo("test");
    }

    @Test
    void addEntryShowsWarningForEmptyValue() {
        bean.setSelected(new Werteliste());
        bean.setNewValue("  ");

        bean.addEntry();

        verify(facesContext).addMessage(isNull(), any(FacesMessage.class));
    }

    @Test
    void addEntryShowsWarningForNullValue() {
        bean.setSelected(new Werteliste());
        bean.setNewValue(null);

        bean.addEntry();

        verify(facesContext).addMessage(isNull(), any(FacesMessage.class));
    }

    @Test
    void addEntryAddsToList() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        bean.setSelected(wl);
        bean.setNewValue("Rot");

        bean.addEntry();

        assertThat(wl.getEntries()).hasSize(1);
        assertThat(wl.getEntries().get(0).getValue()).isEqualTo("Rot");
        assertThat(wl.getEntries().get(0).getSortOrder()).isZero();
        assertThat(bean.getNewValue()).isEmpty();
    }

    @Test
    void addEntryCalculatesSortOrderFromExisting() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        wl.addEntry(new WertelisteEntry("Eins", 0));
        wl.addEntry(new WertelisteEntry("Zwei", 5));
        bean.setSelected(wl);
        bean.setNewValue("Drei");

        bean.addEntry();

        assertThat(wl.getEntries()).hasSize(3);
        assertThat(wl.getEntries().get(2).getSortOrder()).isEqualTo(6);
    }

    // --- removeEntry() ---

    @Test
    void removeEntryDoesNothingWhenNoSelection() {
        bean.setSelected(null);

        bean.removeEntry(new WertelisteEntry()); // should not throw
    }

    @Test
    void removeEntryRemovesFromList() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry entry = new WertelisteEntry("Rot", 0);
        wl.addEntry(entry);
        bean.setSelected(wl);

        bean.removeEntry(entry);

        assertThat(wl.getEntries()).isEmpty();
    }

    // --- moveUp() ---

    @Test
    void moveUpDoesNothingWhenNoSelection() {
        bean.setSelected(null);

        bean.moveUp(new WertelisteEntry()); // should not throw
    }

    @Test
    void moveUpDoesNothingForFirstElement() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry first = new WertelisteEntry("Rot", 0);
        WertelisteEntry second = new WertelisteEntry("Blau", 1);
        wl.addEntry(first);
        wl.addEntry(second);
        bean.setSelected(wl);

        bean.moveUp(first);

        assertThat(wl.getEntries().get(0).getValue()).isEqualTo("Rot");
        assertThat(wl.getEntries().get(1).getValue()).isEqualTo("Blau");
    }

    @Test
    void moveUpMovesElementOnePositionUp() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry first = new WertelisteEntry("Rot", 0);
        WertelisteEntry second = new WertelisteEntry("Blau", 1);
        wl.addEntry(first);
        wl.addEntry(second);
        bean.setSelected(wl);

        bean.moveUp(second);

        assertThat(wl.getEntries().get(0).getValue()).isEqualTo("Blau");
        assertThat(wl.getEntries().get(1).getValue()).isEqualTo("Rot");
        // Check reordering
        assertThat(wl.getEntries().get(0).getSortOrder()).isZero();
        assertThat(wl.getEntries().get(1).getSortOrder()).isEqualTo(1);
    }

    // --- moveDown() ---

    @Test
    void moveDownDoesNothingWhenNoSelection() {
        bean.setSelected(null);

        bean.moveDown(new WertelisteEntry()); // should not throw
    }

    @Test
    void moveDownDoesNothingForLastElement() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry first = new WertelisteEntry("Rot", 0);
        WertelisteEntry second = new WertelisteEntry("Blau", 1);
        wl.addEntry(first);
        wl.addEntry(second);
        bean.setSelected(wl);

        bean.moveDown(second);

        assertThat(wl.getEntries().get(0).getValue()).isEqualTo("Rot");
        assertThat(wl.getEntries().get(1).getValue()).isEqualTo("Blau");
    }

    @Test
    void moveDownMovesElementOnePositionDown() {
        Werteliste wl = new Werteliste();
        wl.setEntries(new ArrayList<>());
        WertelisteEntry first = new WertelisteEntry("Rot", 0);
        WertelisteEntry second = new WertelisteEntry("Blau", 1);
        wl.addEntry(first);
        wl.addEntry(second);
        bean.setSelected(wl);

        bean.moveDown(first);

        assertThat(wl.getEntries().get(0).getValue()).isEqualTo("Blau");
        assertThat(wl.getEntries().get(1).getValue()).isEqualTo("Rot");
        assertThat(wl.getEntries().get(0).getSortOrder()).isZero();
        assertThat(wl.getEntries().get(1).getSortOrder()).isEqualTo(1);
    }

    // --- getAllMandate() ---

    @Test
    void getAllMandateReturnsListFromSecurity() {
        when(security.getAllMandate()).thenReturn(Set.of("mandatA", "mandatB"));

        List<String> result = bean.getAllMandate();

        assertThat(result).containsExactlyInAnyOrder("mandatA", "mandatB");
    }

    // --- getter/setter coverage ---

    @Test
    void gettersAndSettersWork() {
        bean.setNewKey("testKey");
        assertThat(bean.getNewKey()).isEqualTo("testKey");

        bean.setNewValue("testValue");
        assertThat(bean.getNewValue()).isEqualTo("testValue");

        Werteliste wl = new Werteliste();
        bean.setSelected(wl);
        assertThat(bean.getSelected()).isSameAs(wl);

        List<Werteliste> list = new ArrayList<>();
        bean.setWertelisten(list);
        assertThat(bean.getWertelisten()).isSameAs(list);

        bean.setRoot(true);
        assertThat(bean.isRoot()).isTrue();
    }
}
