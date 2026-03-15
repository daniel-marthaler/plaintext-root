/*
 * Copyright (C) plaintext.ch, 2026.
 */
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
