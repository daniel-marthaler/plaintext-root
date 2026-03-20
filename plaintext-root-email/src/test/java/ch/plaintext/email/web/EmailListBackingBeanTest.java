/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailAttachment;
import ch.plaintext.email.service.EmailService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailListBackingBeanTest {

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailListBackingBean bean;

    @Mock
    private FacesContext facesContext;

    private MockedStatic<FacesContext> facesContextMock;

    @BeforeEach
    void setUp() {
        facesContextMock = mockStatic(FacesContext.class);
        facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
    }

    @AfterEach
    void tearDown() {
        facesContextMock.close();
    }

    // --- loadEmails ---

    @Nested
    class LoadEmails {

        @Test
        void loadsEmailsForMandate() {
            when(plaintextSecurity.getMandat()).thenReturn("TEST");
            Email e1 = new Email();
            e1.setId(1L);
            Email e2 = new Email();
            e2.setId(2L);
            when(emailService.getEmailsForMandate("TEST")).thenReturn(List.of(e1, e2));

            bean.loadEmails();

            assertThat(bean.getEmails()).hasSize(2);
            verify(emailService).getEmailsForMandate("TEST");
        }

        @Test
        void clearsExistingEmailsBeforeLoading() {
            when(plaintextSecurity.getMandat()).thenReturn("TEST");
            when(emailService.getEmailsForMandate("TEST")).thenReturn(List.of(new Email()));

            bean.loadEmails();
            assertThat(bean.getEmails()).hasSize(1);

            when(emailService.getEmailsForMandate("TEST")).thenReturn(List.of(new Email(), new Email(), new Email()));
            bean.loadEmails();
            assertThat(bean.getEmails()).hasSize(3);
        }

        @Test
        void handlesNullPlaintextSecurity() {
            bean.setPlaintextSecurity(null);

            bean.loadEmails();

            assertThat(bean.getEmails()).isEmpty();
            verify(emailService, never()).getEmailsForMandate(anyString());
        }
    }

    // --- newEmail ---

    @Test
    void newEmailCreatesOutgoingDraft() {
        when(plaintextSecurity.getMandat()).thenReturn("MANDAT1");

        bean.newEmail();

        Email selected = bean.getSelectedEmail();
        assertThat(selected).isNotNull();
        assertThat(selected.getMandat()).isEqualTo("MANDAT1");
        assertThat(selected.getStatus()).isEqualTo(Email.EmailStatus.DRAFT);
        assertThat(selected.getDirection()).isEqualTo(Email.EmailDirection.OUTGOING);
        assertThat(selected.isHtml()).isFalse();
    }

    @Test
    void newEmailUsesDefaultMandatWhenSecurityNull() {
        bean.setPlaintextSecurity(null);

        bean.newEmail();

        assertThat(bean.getSelectedEmail().getMandat()).isEqualTo("1");
    }

    // --- selectEmail / clearSelection ---

    @Test
    void selectEmailSetsSelectedEmail() {
        Email email = new Email();
        email.setId(5L);
        bean.setSelectedEmail(email);

        bean.selectEmail();

        assertThat(bean.getSelectedEmail()).isNotNull();
        assertThat(bean.getSelectedEmail().getId()).isEqualTo(5L);
    }

    @Test
    void clearSelectionResetsSelectedEmail() {
        bean.setSelectedEmail(new Email());

        bean.clearSelection();

        assertThat(bean.getSelectedEmail()).isNull();
    }

    // --- saveEmail ---

    @Nested
    class SaveEmail {

        @Test
        void addsErrorWhenNoSelectedEmail() {
            bean.setSelectedEmail(null);

            bean.saveEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void createsNewDraftWhenIdNull() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            Email newEmail = new Email();
            newEmail.setToAddress("to@test.com");
            newEmail.setSubject("Subj");
            newEmail.setBody("Body");
            newEmail.setHtml(false);
            bean.setSelectedEmail(newEmail);

            Email saved = new Email();
            saved.setId(10L);
            when(emailService.createDraft("M1", "to@test.com", "Subj", "Body", false))
                    .thenReturn(saved);
            when(emailService.getEmailsForMandate("M1")).thenReturn(List.of());

            bean.saveEmail();

            verify(emailService).createDraft("M1", "to@test.com", "Subj", "Body", false);
            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        }

        @Test
        void updatesExistingDraft() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            Email existing = new Email();
            existing.setId(5L);
            existing.setStatus(Email.EmailStatus.DRAFT);
            existing.setToAddress("to@test.com");
            existing.setCcAddress("cc@test.com");
            existing.setSubject("Subj");
            existing.setBody("Body");
            existing.setHtml(true);
            bean.setSelectedEmail(existing);

            Email updated = new Email();
            updated.setId(5L);
            when(emailService.updateDraft(5L, "to@test.com", "cc@test.com", "Subj", "Body", true))
                    .thenReturn(updated);
            when(emailService.getEmailsForMandate("M1")).thenReturn(List.of());

            bean.saveEmail();

            verify(emailService).updateDraft(5L, "to@test.com", "cc@test.com", "Subj", "Body", true);
        }

        @Test
        void warnsWhenUpdatingNonDraft() {
            Email existing = new Email();
            existing.setId(5L);
            existing.setStatus(Email.EmailStatus.SENT);
            bean.setSelectedEmail(existing);

            when(plaintextSecurity.getMandat()).thenReturn("M1");
            when(emailService.getEmailsForMandate("M1")).thenReturn(List.of());

            bean.saveEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext, atLeastOnce()).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getAllValues().stream()
                    .anyMatch(m -> m.getSeverity() == FacesMessage.SEVERITY_WARN)).isTrue();
        }

        @Test
        void handlesExceptionDuringSave() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            Email newEmail = new Email();
            newEmail.setToAddress("to@test.com");
            newEmail.setSubject("S");
            newEmail.setBody("B");
            newEmail.setHtml(false);
            bean.setSelectedEmail(newEmail);

            when(emailService.createDraft("M1", "to@test.com", "S", "B", false))
                    .thenThrow(new RuntimeException("DB error"));

            bean.saveEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }
    }

    // --- queueEmail ---

    @Nested
    class QueueEmail {

        @Test
        void addsErrorWhenNoSelectedEmail() {
            bean.setSelectedEmail(null);

            bean.queueEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void addsErrorWhenSelectedEmailHasNullId() {
            bean.setSelectedEmail(new Email());

            bean.queueEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void queuesEmailSuccessfully() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            Email email = new Email();
            email.setId(10L);
            bean.setSelectedEmail(email);

            when(emailService.getEmailsForMandate("M1")).thenReturn(List.of());

            bean.queueEmail();

            verify(emailService).queueEmail(10L);
            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        }

        @Test
        void handlesExceptionDuringQueue() {
            Email email = new Email();
            email.setId(10L);
            bean.setSelectedEmail(email);

            doThrow(new RuntimeException("Queue failed")).when(emailService).queueEmail(10L);

            bean.queueEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }
    }

    // --- deleteEmail ---

    @Nested
    class DeleteEmail {

        @Test
        void addsErrorWhenNoSelectedEmail() {
            bean.setSelectedEmail(null);

            bean.deleteEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void addsErrorWhenSelectedEmailHasNullId() {
            bean.setSelectedEmail(new Email());

            bean.deleteEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void deletesEmailSuccessfully() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            Email email = new Email();
            email.setId(10L);
            bean.setSelectedEmail(email);

            when(emailService.getEmailsForMandate("M1")).thenReturn(List.of());

            bean.deleteEmail();

            verify(emailService).deleteEmail(10L);
            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        }

        @Test
        void handlesExceptionDuringDelete() {
            Email email = new Email();
            email.setId(10L);
            bean.setSelectedEmail(email);

            doThrow(new RuntimeException("Delete failed")).when(emailService).deleteEmail(10L);

            bean.deleteEmail();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }
    }

    // --- count methods ---

    @Test
    void getQueuedCountDelegatesToService() {
        when(plaintextSecurity.getMandat()).thenReturn("M1");
        when(emailService.getQueuedCount("M1")).thenReturn(5L);

        assertThat(bean.getQueuedCount()).isEqualTo(5L);
    }

    @Test
    void getSentCountDelegatesToService() {
        when(plaintextSecurity.getMandat()).thenReturn("M1");
        when(emailService.getSentCount("M1")).thenReturn(10L);

        assertThat(bean.getSentCount()).isEqualTo(10L);
    }

    @Test
    void getFailedCountDelegatesToService() {
        when(plaintextSecurity.getMandat()).thenReturn("M1");
        when(emailService.getFailedCount("M1")).thenReturn(3L);

        assertThat(bean.getFailedCount()).isEqualTo(3L);
    }

    // --- formatDateTime ---

    @Test
    void formatDateTimeFormatsCorrectly() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 14, 30);

        String result = bean.formatDateTime(dateTime);

        assertThat(result).isEqualTo("15.06.2024 14:30");
    }

    @Test
    void formatDateTimeReturnsEmptyForNull() {
        assertThat(bean.formatDateTime(null)).isEmpty();
    }

    // --- downloadAttachment ---

    @Nested
    class DownloadAttachment {

        @Test
        void returnsEarlyWhenAttachmentNull() {
            bean.downloadAttachment(null);

            verify(facesContext, never()).getExternalContext();
        }

        @Test
        void returnsEarlyWhenAttachmentDataNull() {
            EmailAttachment attachment = new EmailAttachment();
            attachment.setData(null);

            bean.downloadAttachment(attachment);

            verify(facesContext, never()).getExternalContext();
        }

        @Test
        void downloadsAttachmentWithContentType() throws IOException {
            ExternalContext externalContext = mock(ExternalContext.class);
            when(facesContext.getExternalContext()).thenReturn(externalContext);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            when(externalContext.getResponseOutputStream()).thenReturn(baos);

            EmailAttachment attachment = new EmailAttachment();
            attachment.setFilename("test.pdf");
            attachment.setContentType("application/pdf");
            attachment.setData(new byte[]{1, 2, 3, 4, 5});

            bean.downloadAttachment(attachment);

            verify(externalContext).setResponseContentType("application/pdf");
            verify(externalContext).setResponseContentLength(5);
            verify(facesContext).responseComplete();
            assertThat(baos.toByteArray()).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        void downloadsAttachmentWithNullContentType() throws IOException {
            ExternalContext externalContext = mock(ExternalContext.class);
            when(facesContext.getExternalContext()).thenReturn(externalContext);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            when(externalContext.getResponseOutputStream()).thenReturn(baos);

            EmailAttachment attachment = new EmailAttachment();
            attachment.setFilename("file.bin");
            attachment.setContentType(null);
            attachment.setData(new byte[]{10, 20});

            bean.downloadAttachment(attachment);

            verify(externalContext).setResponseContentType("application/octet-stream");
        }

        @Test
        void handlesIOExceptionDuringDownload() throws IOException {
            ExternalContext externalContext = mock(ExternalContext.class);
            when(facesContext.getExternalContext()).thenReturn(externalContext);
            when(externalContext.getResponseOutputStream()).thenThrow(new IOException("Stream error"));

            EmailAttachment attachment = new EmailAttachment();
            attachment.setFilename("test.pdf");
            attachment.setData(new byte[]{1, 2, 3});

            bean.downloadAttachment(attachment);

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }
    }

    // --- initThis ---

    @Test
    void initThisCallsLoadEmails() {
        when(plaintextSecurity.getMandat()).thenReturn("M1");
        when(emailService.getEmailsForMandate("M1")).thenReturn(List.of());

        bean.initThis();

        verify(emailService).getEmailsForMandate("M1");
    }
}
