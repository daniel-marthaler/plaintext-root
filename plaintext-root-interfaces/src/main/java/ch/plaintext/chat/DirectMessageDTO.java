/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data transfer object representing a direct message between two users.
 * Direct messages are private, one-to-one communications outside of chat rooms.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class DirectMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the direct message. */
    private Long id;
    /** The email address of the message sender. */
    private String senderEmail;
    /** The email address of the message recipient. */
    private String recipientEmail;
    /** The content of the message. */
    private String messageText;
    /** The timestamp when the message was sent. */
    private Date sentAt;
    /** Whether the message has been read by the recipient. */
    private Boolean isRead;
}
