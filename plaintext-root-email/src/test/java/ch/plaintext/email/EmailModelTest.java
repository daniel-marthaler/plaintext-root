/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.email;

import ch.plaintext.email.model.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailModelTest {

    private Email email;

    @BeforeEach
    void setUp() {
        email = new Email();
    }

    @Test
    void prePersist_shouldSetCreatedAtWhenNull() {
        // Given
        email.setCreatedAt(null);

        // When
        email.prePersist();

        // Then
        assertThat(email.getCreatedAt()).isNotNull();
        assertThat(email.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void prePersist_shouldNotOverrideExistingCreatedAt() {
        // Given
        LocalDateTime existingTime = LocalDateTime.now().minusDays(1);
        email.setCreatedAt(existingTime);

        // When
        email.prePersist();

        // Then
        assertThat(email.getCreatedAt()).isEqualTo(existingTime);
    }

    @Test
    void setStatus_shouldChangeEmailStatus() {
        // Given
        email.setStatus(Email.EmailStatus.DRAFT);

        // When
        email.setStatus(Email.EmailStatus.QUEUED);

        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.QUEUED);
    }

    @Test
    void statusTransition_fromDraftToQueued_shouldWork() {
        // Given
        email.setStatus(Email.EmailStatus.DRAFT);

        // When
        email.setStatus(Email.EmailStatus.QUEUED);

        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.QUEUED);
    }

    @Test
    void statusTransition_fromQueuedToSending_shouldWork() {
        // Given
        email.setStatus(Email.EmailStatus.QUEUED);

        // When
        email.setStatus(Email.EmailStatus.SENDING);

        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.SENDING);
    }

    @Test
    void statusTransition_fromSendingToSent_shouldWork() {
        // Given
        email.setStatus(Email.EmailStatus.SENDING);

        // When
        email.setStatus(Email.EmailStatus.SENT);
        email.setSentAt(LocalDateTime.now());

        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.SENT);
        assertThat(email.getSentAt()).isNotNull();
    }

    @Test
    void statusTransition_fromQueuedToFailed_shouldWork() {
        // Given
        email.setStatus(Email.EmailStatus.QUEUED);

        // When
        email.setStatus(Email.EmailStatus.FAILED);
        email.setErrorMessage("Connection timeout");

        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.FAILED);
        assertThat(email.getErrorMessage()).isEqualTo("Connection timeout");
    }

    @Test
    void setMandat_shouldStoreMandat() {
        // When
        email.setMandat("TEST_MANDAT");

        // Then
        assertThat(email.getMandat()).isEqualTo("TEST_MANDAT");
    }

    @Test
    void setFromAddress_shouldStoreAddress() {
        // When
        email.setFromAddress("from@example.com");

        // Then
        assertThat(email.getFromAddress()).isEqualTo("from@example.com");
    }

    @Test
    void setToAddress_shouldStoreAddress() {
        // When
        email.setToAddress("to@example.com");

        // Then
        assertThat(email.getToAddress()).isEqualTo("to@example.com");
    }

    @Test
    void setCcAddress_shouldStoreAddress() {
        // When
        email.setCcAddress("cc@example.com");

        // Then
        assertThat(email.getCcAddress()).isEqualTo("cc@example.com");
    }

    @Test
    void setBccAddress_shouldStoreAddress() {
        // When
        email.setBccAddress("bcc@example.com");

        // Then
        assertThat(email.getBccAddress()).isEqualTo("bcc@example.com");
    }

    @Test
    void setSubject_shouldStoreSubject() {
        // When
        email.setSubject("Test Subject");

        // Then
        assertThat(email.getSubject()).isEqualTo("Test Subject");
    }

    @Test
    void setBody_shouldStoreBody() {
        // When
        email.setBody("Test body content");

        // Then
        assertThat(email.getBody()).isEqualTo("Test body content");
    }

    @Test
    void setHtml_shouldStoreHtmlFlag() {
        // When
        email.setHtml(true);

        // Then
        assertThat(email.isHtml()).isTrue();

        // When
        email.setHtml(false);

        // Then
        assertThat(email.isHtml()).isFalse();
    }

    @Test
    void defaultHtmlFlag_shouldBeFalse() {
        // Then
        assertThat(email.isHtml()).isFalse();
    }

    @Test
    void defaultStatus_shouldBeDraft() {
        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.DRAFT);
    }

    @Test
    void defaultDirection_shouldBeOutgoing() {
        // Then
        assertThat(email.getDirection()).isEqualTo(Email.EmailDirection.OUTGOING);
    }

    @Test
    void defaultRetryCount_shouldBeZero() {
        // Then
        assertThat(email.getRetryCount()).isZero();
    }

    @Test
    void defaultMaxRetries_shouldBeThree() {
        // Then
        assertThat(email.getMaxRetries()).isEqualTo(3);
    }

    @Test
    void setRetryCount_shouldIncrementCorrectly() {
        // Given
        email.setRetryCount(0);

        // When
        email.setRetryCount(email.getRetryCount() + 1);

        // Then
        assertThat(email.getRetryCount()).isEqualTo(1);

        // When
        email.setRetryCount(email.getRetryCount() + 1);

        // Then
        assertThat(email.getRetryCount()).isEqualTo(2);
    }

    @Test
    void setErrorMessage_shouldStoreErrorMessage() {
        // When
        email.setErrorMessage("SMTP connection failed");

        // Then
        assertThat(email.getErrorMessage()).isEqualTo("SMTP connection failed");
    }

    @Test
    void setMessageId_shouldStoreMessageId() {
        // When
        email.setMessageId("<123@example.com>");

        // Then
        assertThat(email.getMessageId()).isEqualTo("<123@example.com>");
    }

    @Test
    void setSentAt_shouldStoreTimestamp() {
        // Given
        LocalDateTime sentTime = LocalDateTime.now();

        // When
        email.setSentAt(sentTime);

        // Then
        assertThat(email.getSentAt()).isEqualTo(sentTime);
    }

    @Test
    void setReceivedAt_shouldStoreTimestamp() {
        // Given
        LocalDateTime receivedTime = LocalDateTime.now();

        // When
        email.setReceivedAt(receivedTime);

        // Then
        assertThat(email.getReceivedAt()).isEqualTo(receivedTime);
    }

    @Test
    void setDirection_toIncoming_shouldWork() {
        // When
        email.setDirection(Email.EmailDirection.INCOMING);

        // Then
        assertThat(email.getDirection()).isEqualTo(Email.EmailDirection.INCOMING);
    }

    @Test
    void setDirection_toOutgoing_shouldWork() {
        // When
        email.setDirection(Email.EmailDirection.OUTGOING);

        // Then
        assertThat(email.getDirection()).isEqualTo(Email.EmailDirection.OUTGOING);
    }

    @Test
    void emailStatus_allEnumValues_shouldExist() {
        // Verify all status values exist
        assertThat(Email.EmailStatus.DRAFT).isNotNull();
        assertThat(Email.EmailStatus.QUEUED).isNotNull();
        assertThat(Email.EmailStatus.SENDING).isNotNull();
        assertThat(Email.EmailStatus.SENT).isNotNull();
        assertThat(Email.EmailStatus.FAILED).isNotNull();
        assertThat(Email.EmailStatus.RECEIVED).isNotNull();
    }

    @Test
    void emailDirection_allEnumValues_shouldExist() {
        // Verify all direction values exist
        assertThat(Email.EmailDirection.INCOMING).isNotNull();
        assertThat(Email.EmailDirection.OUTGOING).isNotNull();
    }

    @Test
    void setMaxRetries_shouldAllowCustomValue() {
        // When
        email.setMaxRetries(5);

        // Then
        assertThat(email.getMaxRetries()).isEqualTo(5);
    }

    @Test
    void multipleRecipientsInToAddress_shouldStoreAsCommaSeparated() {
        // When
        email.setToAddress("to1@example.com, to2@example.com");

        // Then
        assertThat(email.getToAddress()).isEqualTo("to1@example.com, to2@example.com");
    }

    @Test
    void longBodyContent_shouldBeStoredInLobColumn() {
        // Given
        String longBody = "A".repeat(10000);

        // When
        email.setBody(longBody);

        // Then
        assertThat(email.getBody()).hasSize(10000);
    }

    @Test
    void longErrorMessage_shouldBeStoredInLobColumn() {
        // Given
        String longError = "Error: " + "X".repeat(5000);

        // When
        email.setErrorMessage(longError);

        // Then
        assertThat(email.getErrorMessage()).hasSize(5007); // "Error: " is 7 chars + 5000 X's
    }

    @Test
    void setId_shouldStoreId() {
        // When
        email.setId(123L);

        // Then
        assertThat(email.getId()).isEqualTo(123L);
    }

    @Test
    void newEmail_shouldHaveNullId() {
        // Then
        assertThat(email.getId()).isNull();
    }

    @Test
    void failedEmail_withRetryCount_shouldTrackRetries() {
        // Given
        email.setStatus(Email.EmailStatus.FAILED);
        email.setRetryCount(2);
        email.setErrorMessage("Second retry failed");

        // Then
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.FAILED);
        assertThat(email.getRetryCount()).isEqualTo(2);
        assertThat(email.getErrorMessage()).isNotNull();
    }

    @Test
    void receivedEmail_shouldHaveIncomingDirectionAndReceivedStatus() {
        // When
        email.setDirection(Email.EmailDirection.INCOMING);
        email.setStatus(Email.EmailStatus.RECEIVED);
        email.setReceivedAt(LocalDateTime.now());

        // Then
        assertThat(email.getDirection()).isEqualTo(Email.EmailDirection.INCOMING);
        assertThat(email.getStatus()).isEqualTo(Email.EmailStatus.RECEIVED);
        assertThat(email.getReceivedAt()).isNotNull();
    }
}
