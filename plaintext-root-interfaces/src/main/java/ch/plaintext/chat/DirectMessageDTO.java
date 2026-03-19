/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Direct Message DTO
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class DirectMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String senderEmail;
    private String recipientEmail;
    private String messageText;
    private Date sentAt;
    private Boolean isRead;
}
