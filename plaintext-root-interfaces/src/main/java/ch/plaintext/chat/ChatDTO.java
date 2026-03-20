/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data transfer object representing a chat room.
 * Contains the chat metadata including its owner, mandate,
 * and the list of member email addresses.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The unique identifier of the chat. */
    private Long id;
    /** The display name of the chat. */
    private String name;
    /** The email address of the chat owner. */
    private String owner;
    /** The mandate/tenant this chat belongs to. */
    private String mandat;
    /** The timestamp when the chat was created. */
    private Date createdAt;
    /** The email addresses of all chat members. */
    private List<String> memberEmails = new ArrayList<>();
}
