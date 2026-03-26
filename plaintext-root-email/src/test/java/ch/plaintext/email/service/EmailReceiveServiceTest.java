/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email.service;

import ch.plaintext.PlaintextIncomingEmailListener;
import ch.plaintext.email.model.Email;
import ch.plaintext.email.model.EmailConfig;
import ch.plaintext.email.persistence.EmailRepository;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailReceiveServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private List<PlaintextIncomingEmailListener> emailListeners;

    @InjectMocks
    private EmailReceiveService emailReceiveService;

    // ---------------------------------------------------------------
    // Existing tests (public API / simple paths)
    // ---------------------------------------------------------------

    @Test
    void receiveEmailsReturnsEmptyListWhenNoConfig() {
        when(emailService.getConfigForMandate("test")).thenReturn(Optional.empty());

        List<Email> result = emailReceiveService.receiveEmails("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsFromConfigReturnsEmptyWhenImapNotConfigured() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(false);
        config.setConfigName("test");

        List<Email> result = emailReceiveService.receiveEmailsFromConfig(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsFromConfigReturnsEmptyWhenImapEnabledButNotFullyConfigured() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost(null); // Missing host
        config.setConfigName("test");

        List<Email> result = emailReceiveService.receiveEmailsFromConfig(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsFromConfigHandlesConnectionFailureGracefully() {
        EmailConfig config = new EmailConfig();
        config.setImapEnabled(true);
        config.setImapHost("nonexistent.example.com");
        config.setImapPort(993);
        config.setImapUsername("user");
        config.setImapPassword("pass");
        config.setImapUseSsl(true);
        config.setImapFolder("INBOX");
        config.setConfigName("broken");

        // This should not throw, but return empty list after logging error
        List<Email> result = emailReceiveService.receiveEmailsFromConfig(config);

        assertTrue(result.isEmpty());
    }

    @Test
    void receiveEmailsCatchesExceptions() {
        when(emailService.getConfigForMandate("test"))
                .thenThrow(new RuntimeException("DB error"));

        // Should not throw
        List<Email> result = emailReceiveService.receiveEmails("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void testImapConnectionThrowsOnInvalidConfig() {
        EmailConfig config = new EmailConfig();
        config.setImapHost("nonexistent.example.com");
        config.setImapPort(993);
        config.setImapUsername("user");
        config.setImapPassword("pass");
        config.setImapUseSsl(true);

        // testImapConnection does NOT catch exceptions, so this should throw
        assertThrows(Exception.class,
                () -> emailReceiveService.testImapConnection(config));
    }

    // ---------------------------------------------------------------
    // Helper: invoke private methods via reflection
    // ---------------------------------------------------------------

    private Email invokeConvertToEmail(Message message, String mandat) throws Exception {
        Method method = EmailReceiveService.class.getDeclaredMethod("convertToEmail", Message.class, String.class);
        method.setAccessible(true);
        return (Email) method.invoke(emailReceiveService, message, mandat);
    }

    private void invokeProcessMessageContent(Message message, Email email) throws Exception {
        Method method = EmailReceiveService.class.getDeclaredMethod("processMessageContent", Message.class, Email.class);
        method.setAccessible(true);
        method.invoke(emailReceiveService, message, email);
    }

    private boolean invokeProcessMultipart(MimeMultipart mimeMultipart, StringBuilder bodyBuilder, Email email) throws Exception {
        Method method = EmailReceiveService.class.getDeclaredMethod("processMultipart", MimeMultipart.class, StringBuilder.class, Email.class);
        method.setAccessible(true);
        return (boolean) method.invoke(emailReceiveService, mimeMultipart, bodyBuilder, email);
    }

    private void invokeProcessAttachment(BodyPart bodyPart, Email email) throws Exception {
        Method method = EmailReceiveService.class.getDeclaredMethod("processAttachment", BodyPart.class, Email.class);
        method.setAccessible(true);
        method.invoke(emailReceiveService, bodyPart, email);
    }

    private String invokeInternetAddressArrayToString(Address[] addresses) throws Exception {
        Method method = EmailReceiveService.class.getDeclaredMethod("InternetAddressArrayToString", Address[].class);
        method.setAccessible(true);
        return (String) method.invoke(emailReceiveService, (Object) addresses);
    }

    private void invokeNotifyListeners(Email email, String configName) throws Exception {
        Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
        method.setAccessible(true);
        method.invoke(emailReceiveService, email, configName);
    }

    // ---------------------------------------------------------------
    // Tests for convertToEmail
    // ---------------------------------------------------------------

    @Nested
    class ConvertToEmailTests {

        @Test
        void setsBasicFieldsCorrectly() throws Exception {
            Message message = mock(Message.class);
            Address fromAddress = new InternetAddress("sender@example.com");
            Address toAddress = new InternetAddress("recipient@example.com");

            when(message.getFrom()).thenReturn(new Address[]{fromAddress});
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[]{toAddress});
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(null);
            when(message.getSubject()).thenReturn("Test Subject");
            when(message.getHeader("Message-ID")).thenReturn(new String[]{"<msg-123@example.com>"});
            when(message.getReceivedDate()).thenReturn(new java.util.Date());
            when(message.isMimeType("text/plain")).thenReturn(true);
            when(message.getContent()).thenReturn("Hello World");

            Email email = invokeConvertToEmail(message, "testMandat");

            assertEquals("testMandat", email.getMandat());
            assertEquals(Email.EmailDirection.INCOMING, email.getDirection());
            assertEquals(Email.EmailStatus.RECEIVED, email.getStatus());
            assertEquals("sender@example.com", email.getFromAddress());
            assertEquals("recipient@example.com", email.getToAddress());
            assertNull(email.getCcAddress());
            assertEquals("Test Subject", email.getSubject());
            assertEquals("<msg-123@example.com>", email.getMessageId());
            assertNotNull(email.getReceivedAt());
            assertEquals("Hello World", email.getBody());
            assertFalse(email.isHtml());
        }

        @Test
        void handlesNullFromAddress() throws Exception {
            Message message = mock(Message.class);
            when(message.getFrom()).thenReturn(null);
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(null);
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(null);
            when(message.getSubject()).thenReturn(null);
            when(message.getHeader("Message-ID")).thenReturn(null);
            when(message.getReceivedDate()).thenReturn(null);
            when(message.isMimeType("text/plain")).thenReturn(false);
            when(message.isMimeType("text/html")).thenReturn(false);
            when(message.isMimeType("multipart/*")).thenReturn(false);

            Email email = invokeConvertToEmail(message, "m1");

            assertNull(email.getFromAddress());
            assertNull(email.getToAddress());
            assertNull(email.getCcAddress());
            assertNull(email.getSubject());
            assertNull(email.getMessageId());
            // receivedAt should fall back to now when message.getReceivedDate() is null
            assertNotNull(email.getReceivedAt());
        }

        @Test
        void handlesEmptyFromAddressArray() throws Exception {
            Message message = mock(Message.class);
            when(message.getFrom()).thenReturn(new Address[]{});
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(null);
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(null);
            when(message.getSubject()).thenReturn("sub");
            when(message.getHeader("Message-ID")).thenReturn(null);
            when(message.getReceivedDate()).thenReturn(null);
            when(message.isMimeType("text/plain")).thenReturn(true);
            when(message.getContent()).thenReturn("");

            Email email = invokeConvertToEmail(message, "m");

            // from.length == 0, so the if-branch is skipped
            assertNull(email.getFromAddress());
        }

        @Test
        void parsesCcAddresses() throws Exception {
            Message message = mock(Message.class);
            Address from = new InternetAddress("a@b.com");
            Address to = new InternetAddress("c@d.com");
            Address cc1 = new InternetAddress("cc1@d.com");
            Address cc2 = new InternetAddress("cc2@d.com");

            when(message.getFrom()).thenReturn(new Address[]{from});
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(new Address[]{to});
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(new Address[]{cc1, cc2});
            when(message.getSubject()).thenReturn("CC test");
            when(message.getHeader("Message-ID")).thenReturn(null);
            when(message.getReceivedDate()).thenReturn(new java.util.Date());
            when(message.isMimeType("text/plain")).thenReturn(true);
            when(message.getContent()).thenReturn("body");

            Email email = invokeConvertToEmail(message, "m");

            assertEquals("cc1@d.com, cc2@d.com", email.getCcAddress());
        }

        @Test
        void handlesMessageIdHeaderWithMultipleValues() throws Exception {
            Message message = mock(Message.class);
            when(message.getFrom()).thenReturn(new Address[]{new InternetAddress("a@b.com")});
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(null);
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(null);
            when(message.getSubject()).thenReturn("sub");
            when(message.getHeader("Message-ID")).thenReturn(new String[]{"<first@id>", "<second@id>"});
            when(message.getReceivedDate()).thenReturn(new java.util.Date());
            when(message.isMimeType("text/plain")).thenReturn(true);
            when(message.getContent()).thenReturn("");

            Email email = invokeConvertToEmail(message, "m");

            // Should use the first Message-ID
            assertEquals("<first@id>", email.getMessageId());
        }

        @Test
        void handlesEmptyMessageIdHeaderArray() throws Exception {
            Message message = mock(Message.class);
            when(message.getFrom()).thenReturn(null);
            when(message.getRecipients(Message.RecipientType.TO)).thenReturn(null);
            when(message.getRecipients(Message.RecipientType.CC)).thenReturn(null);
            when(message.getSubject()).thenReturn(null);
            when(message.getHeader("Message-ID")).thenReturn(new String[]{});
            when(message.getReceivedDate()).thenReturn(null);
            when(message.isMimeType("text/plain")).thenReturn(false);
            when(message.isMimeType("text/html")).thenReturn(false);
            when(message.isMimeType("multipart/*")).thenReturn(false);

            Email email = invokeConvertToEmail(message, "m");

            assertNull(email.getMessageId());
        }
    }

    // ---------------------------------------------------------------
    // Tests for processMessageContent
    // ---------------------------------------------------------------

    @Nested
    class ProcessMessageContentTests {

        @Test
        void handlesTextPlainContent() throws Exception {
            Message message = mock(Message.class);
            when(message.isMimeType("text/plain")).thenReturn(true);
            when(message.getContent()).thenReturn("Plain text body");

            Email email = new Email();
            invokeProcessMessageContent(message, email);

            assertEquals("Plain text body", email.getBody());
            assertFalse(email.isHtml());
        }

        @Test
        void handlesTextHtmlContent() throws Exception {
            Message message = mock(Message.class);
            when(message.isMimeType("text/plain")).thenReturn(false);
            when(message.isMimeType("text/html")).thenReturn(true);
            when(message.getContent()).thenReturn("<p>HTML body</p>");

            Email email = new Email();
            invokeProcessMessageContent(message, email);

            assertEquals("<p>HTML body</p>", email.getBody());
            assertTrue(email.isHtml());
        }

        @Test
        void handlesMultipartContent() throws Exception {
            Message message = mock(Message.class);
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart textPart = mock(BodyPart.class);

            when(message.isMimeType("text/plain")).thenReturn(false);
            when(message.isMimeType("text/html")).thenReturn(false);
            when(message.isMimeType("multipart/*")).thenReturn(true);
            when(message.getContent()).thenReturn(multipart);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(textPart);
            when(textPart.isMimeType("text/plain")).thenReturn(true);
            when(textPart.getContent()).thenReturn("Multipart plain text");

            Email email = new Email();
            invokeProcessMessageContent(message, email);

            assertEquals("Multipart plain text", email.getBody());
            assertFalse(email.isHtml());
        }

        @Test
        void handlesUnknownMimeType() throws Exception {
            Message message = mock(Message.class);
            when(message.isMimeType("text/plain")).thenReturn(false);
            when(message.isMimeType("text/html")).thenReturn(false);
            when(message.isMimeType("multipart/*")).thenReturn(false);

            Email email = new Email();
            invokeProcessMessageContent(message, email);

            // Body should be empty, html false
            assertEquals("", email.getBody());
            assertFalse(email.isHtml());
        }
    }

    // ---------------------------------------------------------------
    // Tests for processMultipart
    // ---------------------------------------------------------------

    @Nested
    class ProcessMultipartTests {

        @Test
        void processesTextPlainPart() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart bodyPart = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(bodyPart);
            when(bodyPart.isMimeType("text/plain")).thenReturn(true);
            when(bodyPart.getContent()).thenReturn("Plain text");

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertEquals("Plain text", builder.toString());
            assertFalse(hasHtml);
        }

        @Test
        void processesTextHtmlPart() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart bodyPart = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(bodyPart);
            when(bodyPart.isMimeType("text/plain")).thenReturn(false);
            when(bodyPart.isMimeType("text/html")).thenReturn(true);
            when(bodyPart.getContent()).thenReturn("<b>bold</b>");

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertEquals("<b>bold</b>", builder.toString());
            assertTrue(hasHtml);
        }

        @Test
        void processesNestedMultipart() throws Exception {
            MimeMultipart outerMultipart = mock(MimeMultipart.class);
            MimeMultipart innerMultipart = mock(MimeMultipart.class);
            BodyPart outerPart = mock(BodyPart.class);
            BodyPart innerPart = mock(BodyPart.class);

            when(outerMultipart.getCount()).thenReturn(1);
            when(outerMultipart.getBodyPart(0)).thenReturn(outerPart);
            when(outerPart.isMimeType("text/plain")).thenReturn(false);
            when(outerPart.isMimeType("text/html")).thenReturn(false);
            when(outerPart.getContent()).thenReturn(innerMultipart);

            when(innerMultipart.getCount()).thenReturn(1);
            when(innerMultipart.getBodyPart(0)).thenReturn(innerPart);
            when(innerPart.isMimeType("text/plain")).thenReturn(false);
            when(innerPart.isMimeType("text/html")).thenReturn(true);
            when(innerPart.getContent()).thenReturn("<p>nested html</p>");

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(outerMultipart, builder, email);

            assertEquals("<p>nested html</p>", builder.toString());
            assertTrue(hasHtml);
        }

        @Test
        void processesNestedMultipartWithPlainTextReturnsNoHtml() throws Exception {
            MimeMultipart outerMultipart = mock(MimeMultipart.class);
            MimeMultipart innerMultipart = mock(MimeMultipart.class);
            BodyPart outerPart = mock(BodyPart.class);
            BodyPart innerPart = mock(BodyPart.class);

            when(outerMultipart.getCount()).thenReturn(1);
            when(outerMultipart.getBodyPart(0)).thenReturn(outerPart);
            when(outerPart.isMimeType("text/plain")).thenReturn(false);
            when(outerPart.isMimeType("text/html")).thenReturn(false);
            when(outerPart.getContent()).thenReturn(innerMultipart);

            when(innerMultipart.getCount()).thenReturn(1);
            when(innerMultipart.getBodyPart(0)).thenReturn(innerPart);
            when(innerPart.isMimeType("text/plain")).thenReturn(true);
            when(innerPart.getContent()).thenReturn("nested plain");

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(outerMultipart, builder, email);

            assertEquals("nested plain", builder.toString());
            assertFalse(hasHtml);
        }

        @Test
        void processesAttachmentPartByDisposition() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart bodyPart = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(bodyPart);
            when(bodyPart.isMimeType("text/plain")).thenReturn(false);
            when(bodyPart.isMimeType("text/html")).thenReturn(false);
            // Not a nested multipart - return a simple string
            when(bodyPart.getContent()).thenReturn("binary data");
            when(bodyPart.getDisposition()).thenReturn(Part.ATTACHMENT);
            when(bodyPart.getFileName()).thenReturn("file.pdf");
            when(bodyPart.getContentType()).thenReturn("application/pdf");
            when(bodyPart.getInputStream()).thenReturn(new ByteArrayInputStream("pdf-data".getBytes()));

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertFalse(hasHtml);
            assertEquals("", builder.toString());
            assertEquals(1, email.getAttachments().size());
            assertEquals("file.pdf", email.getAttachments().get(0).getFilename());
        }

        @Test
        void processesAttachmentPartByFilenameOnly() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart bodyPart = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(bodyPart);
            when(bodyPart.isMimeType("text/plain")).thenReturn(false);
            when(bodyPart.isMimeType("text/html")).thenReturn(false);
            when(bodyPart.getContent()).thenReturn("data");
            // No attachment disposition but has a filename
            when(bodyPart.getDisposition()).thenReturn(null);
            when(bodyPart.getFileName()).thenReturn("image.png");
            when(bodyPart.getContentType()).thenReturn("image/png");
            when(bodyPart.getInputStream()).thenReturn(new ByteArrayInputStream("png-data".getBytes()));

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertFalse(hasHtml);
            assertEquals(1, email.getAttachments().size());
            assertEquals("image.png", email.getAttachments().get(0).getFilename());
        }

        @Test
        void processesMultiplePartsIncludingTextAndAttachment() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart textPart = mock(BodyPart.class);
            BodyPart htmlPart = mock(BodyPart.class);
            BodyPart attachmentPart = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(3);
            when(multipart.getBodyPart(0)).thenReturn(textPart);
            when(multipart.getBodyPart(1)).thenReturn(htmlPart);
            when(multipart.getBodyPart(2)).thenReturn(attachmentPart);

            // Text part
            when(textPart.isMimeType("text/plain")).thenReturn(true);
            when(textPart.getContent()).thenReturn("Plain ");

            // HTML part
            when(htmlPart.isMimeType("text/plain")).thenReturn(false);
            when(htmlPart.isMimeType("text/html")).thenReturn(true);
            when(htmlPart.getContent()).thenReturn("<b>HTML</b>");

            // Attachment part
            when(attachmentPart.isMimeType("text/plain")).thenReturn(false);
            when(attachmentPart.isMimeType("text/html")).thenReturn(false);
            when(attachmentPart.getContent()).thenReturn("bytes");
            when(attachmentPart.getDisposition()).thenReturn(Part.ATTACHMENT);
            when(attachmentPart.getFileName()).thenReturn("doc.pdf");
            when(attachmentPart.getContentType()).thenReturn("application/pdf");
            when(attachmentPart.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertTrue(hasHtml);
            assertEquals("Plain <b>HTML</b>", builder.toString());
            assertEquals(1, email.getAttachments().size());
        }

        @Test
        void handlesEmptyMultipart() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            when(multipart.getCount()).thenReturn(0);

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertFalse(hasHtml);
            assertEquals("", builder.toString());
            assertTrue(email.getAttachments().isEmpty());
        }

        @Test
        void skipsPartThatIsNeitherTextNorAttachment() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart unknownPart = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(unknownPart);
            when(unknownPart.isMimeType("text/plain")).thenReturn(false);
            when(unknownPart.isMimeType("text/html")).thenReturn(false);
            // Returns something that is not MimeMultipart
            when(unknownPart.getContent()).thenReturn("some binary data");
            when(unknownPart.getDisposition()).thenReturn(null);
            when(unknownPart.getFileName()).thenReturn(null);

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertFalse(hasHtml);
            assertEquals("", builder.toString());
            assertTrue(email.getAttachments().isEmpty());
        }

        @Test
        void skipsPartWithEmptyFilenameAndNoDisposition() throws Exception {
            MimeMultipart multipart = mock(MimeMultipart.class);
            BodyPart part = mock(BodyPart.class);

            when(multipart.getCount()).thenReturn(1);
            when(multipart.getBodyPart(0)).thenReturn(part);
            when(part.isMimeType("text/plain")).thenReturn(false);
            when(part.isMimeType("text/html")).thenReturn(false);
            when(part.getContent()).thenReturn("data");
            when(part.getDisposition()).thenReturn(null);
            when(part.getFileName()).thenReturn("");

            StringBuilder builder = new StringBuilder();
            Email email = new Email();
            boolean hasHtml = invokeProcessMultipart(multipart, builder, email);

            assertFalse(hasHtml);
            assertTrue(email.getAttachments().isEmpty());
        }
    }

    // ---------------------------------------------------------------
    // Tests for processAttachment
    // ---------------------------------------------------------------

    @Nested
    class ProcessAttachmentTests {

        @Test
        void createsAttachmentWithCorrectFields() throws Exception {
            BodyPart bodyPart = mock(BodyPart.class);
            byte[] fileData = "Hello PDF".getBytes();

            when(bodyPart.getFileName()).thenReturn("report.pdf");
            when(bodyPart.getContentType()).thenReturn("application/pdf");
            when(bodyPart.getInputStream()).thenReturn(new ByteArrayInputStream(fileData));

            Email email = new Email();
            invokeProcessAttachment(bodyPart, email);

            assertEquals(1, email.getAttachments().size());
            var att = email.getAttachments().get(0);
            assertEquals("report.pdf", att.getFilename());
            assertEquals("application/pdf", att.getContentType());
            assertEquals((long) fileData.length, att.getSizeBytes());
            assertArrayEquals(fileData, att.getData());
            // addAttachment sets the back-reference
            assertEquals(email, att.getEmail());
        }

        @Test
        void handlesLargeAttachment() throws Exception {
            BodyPart bodyPart = mock(BodyPart.class);
            byte[] largeData = new byte[50_000];
            Arrays.fill(largeData, (byte) 'X');

            when(bodyPart.getFileName()).thenReturn("big.bin");
            when(bodyPart.getContentType()).thenReturn("application/octet-stream");
            when(bodyPart.getInputStream()).thenReturn(new ByteArrayInputStream(largeData));

            Email email = new Email();
            invokeProcessAttachment(bodyPart, email);

            assertEquals(1, email.getAttachments().size());
            assertEquals(50_000L, email.getAttachments().get(0).getSizeBytes());
            assertArrayEquals(largeData, email.getAttachments().get(0).getData());
        }

        @Test
        void handlesExceptionGracefully() throws Exception {
            BodyPart bodyPart = mock(BodyPart.class);
            when(bodyPart.getFileName()).thenReturn("bad.bin");
            when(bodyPart.getContentType()).thenReturn("application/octet-stream");
            when(bodyPart.getInputStream()).thenThrow(new java.io.IOException("read error"));

            Email email = new Email();
            // Should not throw - exception is caught internally
            invokeProcessAttachment(bodyPart, email);

            // No attachment should be added since it failed
            assertTrue(email.getAttachments().isEmpty());
        }

        @Test
        void handlesEmptyAttachment() throws Exception {
            BodyPart bodyPart = mock(BodyPart.class);
            byte[] emptyData = new byte[0];

            when(bodyPart.getFileName()).thenReturn("empty.txt");
            when(bodyPart.getContentType()).thenReturn("text/plain");
            when(bodyPart.getInputStream()).thenReturn(new ByteArrayInputStream(emptyData));

            Email email = new Email();
            invokeProcessAttachment(bodyPart, email);

            assertEquals(1, email.getAttachments().size());
            assertEquals(0L, email.getAttachments().get(0).getSizeBytes());
            assertEquals(0, email.getAttachments().get(0).getData().length);
        }
    }

    // ---------------------------------------------------------------
    // Tests for InternetAddressArrayToString
    // ---------------------------------------------------------------

    @Nested
    class InternetAddressArrayToStringTests {

        @Test
        void singleAddress() throws Exception {
            Address[] addresses = new Address[]{new InternetAddress("one@example.com")};

            String result = invokeInternetAddressArrayToString(addresses);

            assertEquals("one@example.com", result);
        }

        @Test
        void multipleAddresses() throws Exception {
            Address[] addresses = new Address[]{
                    new InternetAddress("a@example.com"),
                    new InternetAddress("b@example.com"),
                    new InternetAddress("c@example.com")
            };

            String result = invokeInternetAddressArrayToString(addresses);

            assertEquals("a@example.com, b@example.com, c@example.com", result);
        }

        @Test
        void twoAddresses() throws Exception {
            Address[] addresses = new Address[]{
                    new InternetAddress("first@test.org"),
                    new InternetAddress("second@test.org")
            };

            String result = invokeInternetAddressArrayToString(addresses);

            assertEquals("first@test.org, second@test.org", result);
        }

        @Test
        void addressWithDisplayName() throws Exception {
            Address[] addresses = new Address[]{
                    new InternetAddress("user@example.com", "John Doe")
            };

            String result = invokeInternetAddressArrayToString(addresses);

            // InternetAddress.toString() includes the display name
            assertTrue(result.contains("John Doe"));
            assertTrue(result.contains("user@example.com"));
        }
    }

    // ---------------------------------------------------------------
    // Tests for notifyListeners
    // ---------------------------------------------------------------

    @Nested
    class NotifyListenersTests {

        @Test
        void doesNothingWhenListenersIsNull() throws Exception {
            // Create a service instance with null listeners using reflection
            EmailReceiveService serviceWithNullListeners = new EmailReceiveService(
                    emailService, emailRepository, null);

            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(1L);
            email.setMandat("m");

            // Should not throw
            method.invoke(serviceWithNullListeners, email, "config1");
        }

        @Test
        void doesNothingWhenListenersIsEmpty() throws Exception {
            EmailReceiveService serviceWithEmptyListeners = new EmailReceiveService(
                    emailService, emailRepository, new ArrayList<>());

            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(1L);
            email.setMandat("m");

            // Should not throw
            method.invoke(serviceWithEmptyListeners, email, "config1");
        }

        @Test
        void notifiesInterestedListener() throws Exception {
            PlaintextIncomingEmailListener listener = mock(PlaintextIncomingEmailListener.class);
            when(listener.getConfigNamesToListenTo()).thenReturn(List.of("myConfig"));
            when(listener.getListenerName()).thenReturn("TestListener");

            List<PlaintextIncomingEmailListener> listeners = new ArrayList<>();
            listeners.add(listener);

            EmailReceiveService service = new EmailReceiveService(emailService, emailRepository, listeners);
            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(42L);
            email.setMandat("testMandat");

            method.invoke(service, email, "myConfig");

            verify(listener).onEmailReceived(42L, "testMandat", "myConfig");
        }

        @Test
        void skipsListenerNotInterestedInConfig() throws Exception {
            PlaintextIncomingEmailListener listener = mock(PlaintextIncomingEmailListener.class);
            when(listener.getConfigNamesToListenTo()).thenReturn(List.of("otherConfig"));
            when(listener.getListenerName()).thenReturn("SkippedListener");

            List<PlaintextIncomingEmailListener> listeners = new ArrayList<>();
            listeners.add(listener);

            EmailReceiveService service = new EmailReceiveService(emailService, emailRepository, listeners);
            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(1L);
            email.setMandat("m");

            method.invoke(service, email, "myConfig");

            verify(listener, never()).onEmailReceived(anyLong(), anyString(), anyString());
        }

        @Test
        void notifiesListenerWithNullConfigList() throws Exception {
            // null configNamesToListenTo means "listen to all"
            PlaintextIncomingEmailListener listener = mock(PlaintextIncomingEmailListener.class);
            when(listener.getConfigNamesToListenTo()).thenReturn(null);
            when(listener.getListenerName()).thenReturn("AllConfigs");

            List<PlaintextIncomingEmailListener> listeners = new ArrayList<>();
            listeners.add(listener);

            EmailReceiveService service = new EmailReceiveService(emailService, emailRepository, listeners);
            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(5L);
            email.setMandat("m");

            method.invoke(service, email, "anyConfig");

            verify(listener).onEmailReceived(5L, "m", "anyConfig");
        }

        @Test
        void notifiesListenerWithEmptyConfigList() throws Exception {
            // empty configNamesToListenTo means "listen to all"
            PlaintextIncomingEmailListener listener = mock(PlaintextIncomingEmailListener.class);
            when(listener.getConfigNamesToListenTo()).thenReturn(Collections.emptyList());
            when(listener.getListenerName()).thenReturn("AllConfigs");

            List<PlaintextIncomingEmailListener> listeners = new ArrayList<>();
            listeners.add(listener);

            EmailReceiveService service = new EmailReceiveService(emailService, emailRepository, listeners);
            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(7L);
            email.setMandat("m");

            method.invoke(service, email, "anyConfig");

            verify(listener).onEmailReceived(7L, "m", "anyConfig");
        }

        @Test
        void handlesListenerExceptionGracefully() throws Exception {
            PlaintextIncomingEmailListener failingListener = mock(PlaintextIncomingEmailListener.class);
            when(failingListener.getConfigNamesToListenTo()).thenReturn(null);
            when(failingListener.getListenerName()).thenReturn("FailingListener");
            doThrow(new RuntimeException("listener error"))
                    .when(failingListener).onEmailReceived(anyLong(), anyString(), anyString());

            PlaintextIncomingEmailListener successListener = mock(PlaintextIncomingEmailListener.class);
            when(successListener.getConfigNamesToListenTo()).thenReturn(null);
            when(successListener.getListenerName()).thenReturn("SuccessListener");

            List<PlaintextIncomingEmailListener> listeners = new ArrayList<>();
            listeners.add(failingListener);
            listeners.add(successListener);

            EmailReceiveService service = new EmailReceiveService(emailService, emailRepository, listeners);
            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(10L);
            email.setMandat("m");

            // Should not throw, and should still notify the second listener
            method.invoke(service, email, "cfg");

            verify(failingListener).onEmailReceived(10L, "m", "cfg");
            verify(successListener).onEmailReceived(10L, "m", "cfg");
        }

        @Test
        void notifiesMultipleInterestedListeners() throws Exception {
            PlaintextIncomingEmailListener listener1 = mock(PlaintextIncomingEmailListener.class);
            when(listener1.getConfigNamesToListenTo()).thenReturn(List.of("cfg1", "cfg2"));
            when(listener1.getListenerName()).thenReturn("L1");

            PlaintextIncomingEmailListener listener2 = mock(PlaintextIncomingEmailListener.class);
            when(listener2.getConfigNamesToListenTo()).thenReturn(List.of("cfg2", "cfg3"));
            when(listener2.getListenerName()).thenReturn("L2");

            PlaintextIncomingEmailListener listener3 = mock(PlaintextIncomingEmailListener.class);
            when(listener3.getConfigNamesToListenTo()).thenReturn(List.of("cfg1"));
            when(listener3.getListenerName()).thenReturn("L3");

            List<PlaintextIncomingEmailListener> listeners = new ArrayList<>();
            listeners.add(listener1);
            listeners.add(listener2);
            listeners.add(listener3);

            EmailReceiveService service = new EmailReceiveService(emailService, emailRepository, listeners);
            Method method = EmailReceiveService.class.getDeclaredMethod("notifyListeners", Email.class, String.class);
            method.setAccessible(true);

            Email email = new Email();
            email.setId(20L);
            email.setMandat("m");

            method.invoke(service, email, "cfg2");

            // listener1 and listener2 are interested in cfg2, listener3 is not
            verify(listener1).onEmailReceived(20L, "m", "cfg2");
            verify(listener2).onEmailReceived(20L, "m", "cfg2");
            verify(listener3, never()).onEmailReceived(anyLong(), anyString(), anyString());
        }
    }
}
