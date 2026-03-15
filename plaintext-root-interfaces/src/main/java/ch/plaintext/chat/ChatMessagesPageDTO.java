/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.chat;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Chat Messages Page Data Transfer Object
 * Contains paginated chat messages with pagination metadata
 *
 * @author info@plaintext.ch
 * @since 2026
 */
@Data
public class ChatMessagesPageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<ChatMessageDTO> messages;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
