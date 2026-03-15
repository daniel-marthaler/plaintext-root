/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Chat Invitation DTO
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatInvitationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chatId;
    private String chatName;
    private String inviterEmail;
    private String inviteeEmail;
    private Date invitedAt;
    private String status;
    private Date respondedAt;
}
