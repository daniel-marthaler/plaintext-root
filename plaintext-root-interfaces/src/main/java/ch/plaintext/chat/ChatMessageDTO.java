/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data transfer object representing a message within a chat room.
 * Contains the message content, sender information, and read status.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the message. */
    private Long id;
    /** The ID of the chat this message belongs to. */
    private Long chatId;
    /** The email address of the message sender. */
    private String senderEmail;
    /** The content of the message. */
    private String messageText;
    /** The timestamp when the message was sent. */
    private Date sentAt;
    /** Whether the message has been read. */
    private Boolean isRead;
}
