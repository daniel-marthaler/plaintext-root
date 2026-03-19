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
 * Chat Data Transfer Object
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String owner;
    private String mandat;
    private Date createdAt;
    private List<String> memberEmails = new ArrayList<>();
}
