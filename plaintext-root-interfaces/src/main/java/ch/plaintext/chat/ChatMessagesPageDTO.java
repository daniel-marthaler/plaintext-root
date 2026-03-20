/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Data transfer object for paginated chat messages.
 * Contains a page of chat messages along with pagination metadata
 * such as current page, total pages, and navigation flags.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatMessagesPageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** The list of chat messages for the current page. */
    private List<ChatMessageDTO> messages;
    /** The current page number (0-based). */
    private int currentPage;
    /** The number of messages per page. */
    private int pageSize;
    /** The total number of messages across all pages. */
    private long totalElements;
    /** The total number of pages. */
    private int totalPages;
    /** Whether there is a next page available. */
    private boolean hasNext;
    /** Whether there is a previous page available. */
    private boolean hasPrevious;
}
