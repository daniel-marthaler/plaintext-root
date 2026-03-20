/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.web;

import ch.plaintext.PlaintextSecurity;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.service.EmailReceiveService;
import ch.plaintext.email.service.EmailSendService;
import ch.plaintext.email.service.EmailService;
import jakarta.faces.application.FacesMessage;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailConfigBackingBeanTest {

    @Mock
    private PlaintextSecurity plaintextSecurity;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailSendService emailSendService;

    @Mock
    private EmailReceiveService emailReceiveService;

    @InjectMocks
    private EmailConfigBackingBean bean;

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

    // --- loadConfigs ---

    @Nested
    class LoadConfigs {

        @Test
        void loadsConfigsForMandate() {
            when(plaintextSecurity.getMandat()).thenReturn("TEST");
            EmailConfig c1 = new EmailConfig();
            c1.setId(1L);
            when(emailService.getConfigsForMandate("TEST")).thenReturn(List.of(c1));

            bean.loadConfigs();

            assertThat(bean.getConfigs()).hasSize(1);
            verify(emailService).getConfigsForMandate("TEST");
        }

        @Test
        void clearsTestResultsOnLoad() {
            when(plaintextSecurity.getMandat()).thenReturn("TEST");
            when(emailService.getConfigsForMandate("TEST")).thenReturn(List.of());

            // Put some test results
            bean.getImapTestResults().put(1L, true);
            bean.getImapTestMessages().put(1L, "OK");
            bean.getSmtpTestResults().put(1L, true);
            bean.getSmtpTestMessages().put(1L, "OK");

            bean.loadConfigs();

            assertThat(bean.getImapTestResults()).isEmpty();
            assertThat(bean.getImapTestMessages()).isEmpty();
            assertThat(bean.getSmtpTestResults()).isEmpty();
            assertThat(bean.getSmtpTestMessages()).isEmpty();
        }

        @Test
        void handlesNullPlaintextSecurity() {
            bean.setPlaintextSecurity(null);

            bean.loadConfigs();

            assertThat(bean.getConfigs()).isEmpty();
            verify(emailService, never()).getConfigsForMandate(anyString());
        }
    }

    // --- newConfig ---

    @Test
    void newConfigCreatesConfigWithDefaults() {
        when(plaintextSecurity.getMandat()).thenReturn("M1");

        bean.newConfig();

        EmailConfig selected = bean.getSelectedConfig();
        assertThat(selected).isNotNull();
        assertThat(selected.getMandat()).isEqualTo("M1");
        assertThat(selected.getSmtpPort()).isEqualTo(587);
        assertThat(selected.isSmtpUseTls()).isTrue();
        assertThat(selected.isSmtpUseSsl()).isFalse();
        assertThat(selected.isSmtpEnabled()).isFalse();
        assertThat(selected.getImapPort()).isEqualTo(993);
        assertThat(selected.isImapUseSsl()).isTrue();
        assertThat(selected.isImapEnabled()).isFalse();
        assertThat(selected.getImapFolder()).isEqualTo("INBOX");
        assertThat(selected.isImapMarkAsRead()).isTrue();
        assertThat(selected.isImapDeleteAfterFetch()).isFalse();
    }

    @Test
    void newConfigUsesDefaultMandatWhenSecurityNull() {
        bean.setPlaintextSecurity(null);

        bean.newConfig();

        assertThat(bean.getSelectedConfig().getMandat()).isEqualTo("1");
    }

    // --- selectConfig / clearSelection ---

    @Test
    void selectConfigSetsSelectedConfig() {
        EmailConfig config = new EmailConfig();
        config.setId(5L);
        bean.setSelectedConfig(config);

        bean.selectConfig();

        assertThat(bean.getSelectedConfig()).isNotNull();
        assertThat(bean.getSelectedConfig().getId()).isEqualTo(5L);
    }

    @Test
    void clearSelectionResetsSelectedConfig() {
        bean.setSelectedConfig(new EmailConfig());

        bean.clearSelection();

        assertThat(bean.getSelectedConfig()).isNull();
    }

    // --- saveConfig ---

    @Nested
    class SaveConfig {

        @Test
        void addsErrorWhenNoSelectedConfig() {
            bean.setSelectedConfig(null);

            bean.saveConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
            verify(facesContext).validationFailed();
        }

        @Test
        void addsErrorWhenConfigNameNull() {
            EmailConfig config = new EmailConfig();
            config.setConfigName(null);
            bean.setSelectedConfig(config);

            bean.saveConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext, atLeastOnce()).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getAllValues().stream()
                    .anyMatch(m -> m.getSeverity() == FacesMessage.SEVERITY_ERROR)).isTrue();
            verify(facesContext).validationFailed();
        }

        @Test
        void addsErrorWhenConfigNameBlank() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("   ");
            bean.setSelectedConfig(config);

            bean.saveConfig();

            verify(facesContext).validationFailed();
        }

        @Test
        void savesConfigSuccessfully() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            EmailConfig config = new EmailConfig();
            config.setConfigName("myconfig");
            config.setMandat("M1");
            bean.setSelectedConfig(config);

            EmailConfig saved = new EmailConfig();
            saved.setId(1L);
            saved.setConfigName("myconfig");
            saved.setMandat("M1");
            when(emailService.saveConfig(config)).thenReturn(saved);
            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.saveConfig();

            verify(emailService).saveConfig(config);
            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        }

        @Test
        void handlesConstraintViolation() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("duplicate");
            config.setMandat("M1");
            bean.setSelectedConfig(config);

            when(emailService.saveConfig(config))
                    .thenThrow(new RuntimeException("constraint violation"));

            bean.saveConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
            assertThat(msgCaptor.getValue().getDetail()).contains("existiert bereits");
            verify(facesContext).validationFailed();
        }

        @Test
        void handlesGenericException() {
            EmailConfig config = new EmailConfig();
            config.setConfigName("test");
            config.setMandat("M1");
            bean.setSelectedConfig(config);

            when(emailService.saveConfig(config))
                    .thenThrow(new RuntimeException("DB error"));

            bean.saveConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
            verify(facesContext).validationFailed();
        }
    }

    // --- deleteConfig ---

    @Nested
    class DeleteConfig {

        @Test
        void addsErrorWhenNoSelectedConfig() {
            bean.setSelectedConfig(null);

            bean.deleteConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void addsErrorWhenSelectedConfigHasNullId() {
            bean.setSelectedConfig(new EmailConfig());

            bean.deleteConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void deletesConfigSuccessfully() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            EmailConfig config = new EmailConfig();
            config.setId(10L);
            bean.setSelectedConfig(config);

            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.deleteConfig();

            verify(emailService).deleteConfig(10L);
            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_INFO);
        }

        @Test
        void handlesExceptionDuringDelete() {
            EmailConfig config = new EmailConfig();
            config.setId(10L);
            bean.setSelectedConfig(config);

            doThrow(new RuntimeException("Delete failed")).when(emailService).deleteConfig(10L);

            bean.deleteConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }
    }

    // --- duplicateConfig ---

    @Nested
    class DuplicateConfig {

        @Test
        void addsErrorWhenNoSelectedConfig() {
            bean.setSelectedConfig(null);

            bean.duplicateConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void addsErrorWhenSelectedConfigHasNullId() {
            bean.setSelectedConfig(new EmailConfig());

            bean.duplicateConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }

        @Test
        void duplicatesConfigSuccessfully() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            EmailConfig original = new EmailConfig();
            original.setId(1L);
            original.setConfigName("myconfig");
            original.setMandat("M1");
            original.setSmtpHost("smtp.test.com");
            original.setSmtpPort(587);
            original.setSmtpUsername("user");
            original.setSmtpPassword("pass");
            original.setSmtpFromAddress("from@test.com");
            original.setSmtpFromName("Test");
            original.setSmtpUseTls(true);
            original.setSmtpUseSsl(false);
            original.setSmtpEnabled(true);
            original.setImapHost("imap.test.com");
            original.setImapPort(993);
            original.setImapUsername("imapuser");
            original.setImapPassword("imappass");
            original.setImapUseSsl(true);
            original.setImapFolder("INBOX");
            original.setImapMarkAsRead(true);
            original.setImapDeleteAfterFetch(false);
            original.setImapEnabled(true);
            bean.setSelectedConfig(original);

            EmailConfig saved = new EmailConfig();
            saved.setId(2L);
            saved.setConfigName("myconfig_2");
            when(emailService.saveConfig(any(EmailConfig.class))).thenReturn(saved);
            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.duplicateConfig();

            ArgumentCaptor<EmailConfig> configCaptor = ArgumentCaptor.forClass(EmailConfig.class);
            verify(emailService).saveConfig(configCaptor.capture());
            EmailConfig duplicated = configCaptor.getValue();
            assertThat(duplicated.getConfigName()).isEqualTo("myconfig_2");
            assertThat(duplicated.getSmtpHost()).isEqualTo("smtp.test.com");
            assertThat(duplicated.getImapHost()).isEqualTo("imap.test.com");
        }

        @Test
        void duplicatesConfigWithExistingSuffix() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            EmailConfig original = new EmailConfig();
            original.setId(1L);
            original.setConfigName("myconfig_3");
            original.setMandat("M1");
            bean.setSelectedConfig(original);

            EmailConfig saved = new EmailConfig();
            saved.setId(2L);
            when(emailService.saveConfig(any(EmailConfig.class))).thenReturn(saved);
            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.duplicateConfig();

            ArgumentCaptor<EmailConfig> configCaptor = ArgumentCaptor.forClass(EmailConfig.class);
            verify(emailService).saveConfig(configCaptor.capture());
            // Should strip "_3" suffix and try "_2"
            assertThat(configCaptor.getValue().getConfigName()).isEqualTo("myconfig_2");
        }

        @Test
        void retriesOnConstraintViolation() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            EmailConfig original = new EmailConfig();
            original.setId(1L);
            original.setConfigName("cfg");
            original.setMandat("M1");
            bean.setSelectedConfig(original);

            // First save fails with constraint, second succeeds
            EmailConfig saved = new EmailConfig();
            saved.setId(2L);
            when(emailService.saveConfig(any(EmailConfig.class)))
                    .thenThrow(new RuntimeException("constraint violation"))
                    .thenReturn(saved);
            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.duplicateConfig();

            // Should have called saveConfig twice (first with _2, then _3)
            verify(emailService, times(2)).saveConfig(any(EmailConfig.class));
        }

        @Test
        void handlesGenericExceptionDuringDuplicate() {
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            EmailConfig original = new EmailConfig();
            original.setId(1L);
            original.setConfigName("cfg");
            original.setMandat("M1");
            bean.setSelectedConfig(original);

            when(emailService.saveConfig(any(EmailConfig.class)))
                    .thenThrow(new RuntimeException("DB connection lost"));

            bean.duplicateConfig();

            ArgumentCaptor<FacesMessage> msgCaptor = ArgumentCaptor.forClass(FacesMessage.class);
            verify(facesContext).addMessage(isNull(), msgCaptor.capture());
            assertThat(msgCaptor.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_ERROR);
        }
    }

    // --- testSmtpForConfig ---

    @Nested
    class TestSmtpForConfig {

        @Test
        void returnsEarlyWhenConfigNotFound() throws Exception {
            bean.setConfigs(List.of());

            bean.testSmtpForConfig(999L);

            verify(emailSendService, never()).sendTestEmail(any(), anyString());
        }

        @Test
        void testSmtpSuccessfully() throws Exception {
            EmailConfig config = new EmailConfig();
            config.setId(1L);
            config.setConfigName("test");
            bean.setConfigs(List.of(config));

            bean.testSmtpForConfig(1L);

            verify(emailSendService).sendTestEmail(config, "daniel@danielmarthaler.ch");
            assertThat(bean.getSmtpTestResults().get(1L)).isTrue();
            assertThat(bean.getSmtpTestMessages().get(1L)).isEqualTo("Gesendet");
        }

        @Test
        void testSmtpHandlesException() throws Exception {
            EmailConfig config = new EmailConfig();
            config.setId(1L);
            config.setConfigName("test");
            bean.setConfigs(List.of(config));

            doThrow(new RuntimeException("SMTP error")).when(emailSendService)
                    .sendTestEmail(any(), anyString());

            bean.testSmtpForConfig(1L);

            assertThat(bean.getSmtpTestResults().get(1L)).isFalse();
            assertThat(bean.getSmtpTestMessages().get(1L)).isEqualTo("Fehler");
        }
    }

    // --- testImapForConfig ---

    @Nested
    class TestImapForConfig {

        @Test
        void returnsEarlyWhenConfigNotFound() throws Exception {
            bean.setConfigs(List.of());

            bean.testImapForConfig(999L);

            verify(emailReceiveService, never()).testImapConnection(any());
        }

        @Test
        void testImapSuccessfully() throws Exception {
            EmailConfig config = new EmailConfig();
            config.setId(1L);
            config.setConfigName("test");
            config.setImapFolder("INBOX");
            bean.setConfigs(List.of(config));

            when(emailReceiveService.testImapConnection(config)).thenReturn(42);

            bean.testImapForConfig(1L);

            assertThat(bean.getImapTestResults().get(1L)).isTrue();
            assertThat(bean.getImapTestMessages().get(1L)).isEqualTo("42 Mails");
        }

        @Test
        void testImapHandlesException() throws Exception {
            EmailConfig config = new EmailConfig();
            config.setId(1L);
            config.setConfigName("test");
            bean.setConfigs(List.of(config));

            when(emailReceiveService.testImapConnection(config))
                    .thenThrow(new RuntimeException("IMAP error"));

            bean.testImapForConfig(1L);

            assertThat(bean.getImapTestResults().get(1L)).isFalse();
            assertThat(bean.getImapTestMessages().get(1L)).isEqualTo("Fehler");
        }
    }

    // --- button labels ---

    @Nested
    class ButtonLabels {

        @Test
        void getImapButtonLabelReturnsTestWhenNoResult() {
            assertThat(bean.getImapButtonLabel(1L)).isEqualTo("Test");
        }

        @Test
        void getImapButtonLabelReturnsTestForNullId() {
            assertThat(bean.getImapButtonLabel(null)).isEqualTo("Test");
        }

        @Test
        void getImapButtonLabelReturnsMessageOnSuccess() {
            bean.getImapTestResults().put(1L, true);
            bean.getImapTestMessages().put(1L, "5 Mails");

            assertThat(bean.getImapButtonLabel(1L)).isEqualTo("5 Mails");
        }

        @Test
        void getImapButtonLabelReturnsFehlerOnFailure() {
            bean.getImapTestResults().put(1L, false);

            assertThat(bean.getImapButtonLabel(1L)).isEqualTo("Fehler");
        }

        @Test
        void getSmtpButtonLabelReturnsTestWhenNoResult() {
            assertThat(bean.getSmtpButtonLabel(1L)).isEqualTo("Test");
        }

        @Test
        void getSmtpButtonLabelReturnsTestForNullId() {
            assertThat(bean.getSmtpButtonLabel(null)).isEqualTo("Test");
        }

        @Test
        void getSmtpButtonLabelReturnsMessageOnSuccess() {
            bean.getSmtpTestResults().put(1L, true);
            bean.getSmtpTestMessages().put(1L, "Gesendet");

            assertThat(bean.getSmtpButtonLabel(1L)).isEqualTo("Gesendet");
        }

        @Test
        void getSmtpButtonLabelReturnsFehlerOnFailure() {
            bean.getSmtpTestResults().put(1L, false);

            assertThat(bean.getSmtpButtonLabel(1L)).isEqualTo("Fehler");
        }
    }

    // --- style classes ---

    @Nested
    class StyleClasses {

        @Test
        void getImapTestStyleClassReturnsOutlinedWhenNoResult() {
            assertThat(bean.getImapTestStyleClass(1L)).isEqualTo("ui-button-outlined");
        }

        @Test
        void getImapTestStyleClassReturnsOutlinedForNullId() {
            assertThat(bean.getImapTestStyleClass(null)).isEqualTo("ui-button-outlined");
        }

        @Test
        void getImapTestStyleClassReturnsSuccessOnSuccess() {
            bean.getImapTestResults().put(1L, true);

            assertThat(bean.getImapTestStyleClass(1L)).isEqualTo("ui-button-success");
        }

        @Test
        void getImapTestStyleClassReturnsDangerOnFailure() {
            bean.getImapTestResults().put(1L, false);

            assertThat(bean.getImapTestStyleClass(1L)).isEqualTo("ui-button-danger");
        }

        @Test
        void getSmtpTestStyleClassReturnsOutlinedWhenNoResult() {
            assertThat(bean.getSmtpTestStyleClass(1L)).isEqualTo("ui-button-outlined");
        }

        @Test
        void getSmtpTestStyleClassReturnsOutlinedForNullId() {
            assertThat(bean.getSmtpTestStyleClass(null)).isEqualTo("ui-button-outlined");
        }

        @Test
        void getSmtpTestStyleClassReturnsSuccessOnSuccess() {
            bean.getSmtpTestResults().put(1L, true);

            assertThat(bean.getSmtpTestStyleClass(1L)).isEqualTo("ui-button-success");
        }

        @Test
        void getSmtpTestStyleClassReturnsDangerOnFailure() {
            bean.getSmtpTestResults().put(1L, false);

            assertThat(bean.getSmtpTestStyleClass(1L)).isEqualTo("ui-button-danger");
        }
    }

    // --- isSmtpConfigured / isImapConfigured ---

    @Test
    void isSmtpConfiguredReturnsFalseWhenNoSelectedConfig() {
        bean.setSelectedConfig(null);

        assertThat(bean.isSmtpConfigured()).isFalse();
    }

    @Test
    void isSmtpConfiguredDelegatesToConfig() {
        EmailConfig config = new EmailConfig();
        config.setSmtpEnabled(true);
        config.setSmtpHost("smtp.test.com");
        config.setSmtpUsername("user");
        config.setSmtpPassword("pass");
        config.setSmtpFromAddress("from@test.com");
        bean.setSelectedConfig(config);

        assertThat(bean.isSmtpConfigured()).isTrue();
    }

    @Test
    void isImapConfiguredReturnsFalseWhenNoSelectedConfig() {
        bean.setSelectedConfig(null);

        assertThat(bean.isImapConfigured()).isFalse();
    }

    @Test
    void isImapConfiguredDelegatesToConfig() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost("imap.test.com");
        config.setImapUsername("user");
        config.setImapPassword("pass");
        bean.setSelectedConfig(config);

        assertThat(bean.isImapConfigured()).isTrue();
    }

    // --- getActiveCronJobCount ---

    @Test
    void getActiveCronJobCountDelegatesToService() {
        when(emailService.getActiveImapConfigCount()).thenReturn(3);

        assertThat(bean.getActiveCronJobCount()).isEqualTo(3);
    }

    // --- initThis ---

    @Nested
    class InitThis {

        @Test
        void loadsConfigsOnNonAjaxRequest() {
            when(facesContext.getPartialViewContext()).thenReturn(mock(jakarta.faces.context.PartialViewContext.class));
            when(facesContext.getPartialViewContext().isAjaxRequest()).thenReturn(false);
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.initThis();

            verify(emailService).getConfigsForMandate("M1");
        }

        @Test
        void skipsLoadOnAjaxRequest() {
            when(facesContext.getPartialViewContext()).thenReturn(mock(jakarta.faces.context.PartialViewContext.class));
            when(facesContext.getPartialViewContext().isAjaxRequest()).thenReturn(true);

            bean.initThis();

            verify(emailService, never()).getConfigsForMandate(anyString());
        }

        @Test
        void loadsConfigsWhenFacesContextIsNull() {
            facesContextMock.when(FacesContext::getCurrentInstance).thenReturn(null);
            when(plaintextSecurity.getMandat()).thenReturn("M1");
            when(emailService.getConfigsForMandate("M1")).thenReturn(List.of());

            bean.initThis();

            verify(emailService).getConfigsForMandate("M1");
        }
    }
}
