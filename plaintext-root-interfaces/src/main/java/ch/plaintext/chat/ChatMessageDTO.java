/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Chat Message Data Transfer Object
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chatId;
    private String senderEmail;
    private String messageText;
    private Date sentAt;
    private Boolean isRead;
}
