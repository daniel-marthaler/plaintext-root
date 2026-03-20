/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Data transfer object representing a chat invitation.
 * Contains information about who invited whom, the target chat,
 * and the current status of the invitation.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatInvitationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the invitation. */
    private Long id;
    /** The ID of the chat the invitation is for. */
    private Long chatId;
    /** The name of the chat the invitation is for. */
    private String chatName;
    /** The email address of the user who sent the invitation. */
    private String inviterEmail;
    /** The email address of the user who received the invitation. */
    private String inviteeEmail;
    /** The timestamp when the invitation was created. */
    private Date invitedAt;
    /** The current status of the invitation (e.g. PENDING, ACCEPTED, DECLINED). */
    private String status;
    /** The timestamp when the invitation was responded to, or null if pending. */
    private Date respondedAt;
}
