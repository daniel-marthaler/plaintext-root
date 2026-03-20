/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import java.util.List;

/**
 * Service interface for managing chat rooms, memberships, messages, and invitations.
 * Provides operations for creating chats, sending messages, and handling
 * user invitations with notification support.
 *
 * @author info@plaintext.ch
 * @since 2026
 */
public interface ChatService {

    /**
     * Create a new chat.
     *
     * @param chatName the name of the chat
     * @param owner    the email address of the chat owner
     * @param mandat   the mandate/tenant identifier
     * @return the created chat DTO
     */
    ChatDTO createChat(String chatName, String owner, String mandat);

    /**
     * Add a member to a chat.
     *
     * @param chatId    the chat ID
     * @param userEmail the email address of the user to add
     */
    void addMember(Long chatId, String userEmail);

    /**
     * Remove a member from a chat.
     *
     * @param chatId    the chat ID
     * @param userEmail the email address of the user to remove
     */
    void removeMember(Long chatId, String userEmail);

    /**
     * Send a message in a chat.
     *
     * @param chatId      the chat ID
     * @param senderEmail the email address of the sender
     * @param messageText the message text
     * @return the created message DTO
     */
    ChatMessageDTO sendMessage(Long chatId, String senderEmail, String messageText);

    /**
     * Get all chats owned by a user.
     *
     * @param owner  the email address of the owner
     * @param mandat the mandate/tenant identifier
     * @return list of owned chat DTOs
     */
    List<ChatDTO> getOwnedChats(String owner, String mandat);

    /**
     * Get all chats where user is a member.
     *
     * @param userEmail the email address of the user
     * @param mandat    the mandate/tenant identifier
     * @return list of chat DTOs the user is a member of
     */
    List<ChatDTO> getChatsByMember(String userEmail, String mandat);

    /**
     * Get all messages in a chat.
     *
     * @param chatId the chat ID
     * @return list of all messages in the chat
     */
    List<ChatMessageDTO> getMessages(Long chatId);

    /**
     * Get messages in a chat with pagination support.
     *
     * @param chatId the chat ID
     * @param page   the page number (0-based)
     * @param size   the page size (number of messages per page)
     * @return paginated messages with pagination metadata
     */
    ChatMessagesPageDTO getMessages(Long chatId, int page, int size);

    /**
     * Get a chat by ID.
     *
     * @param chatId the chat ID
     * @return the chat DTO
     */
    ChatDTO getChat(Long chatId);

    /**
     * Delete a chat and all its messages.
     *
     * @param chatId the chat ID
     */
    void deleteChat(Long chatId);

    /**
     * Check if a user is a member of a chat.
     *
     * @param chatId    the chat ID
     * @param userEmail the email address of the user
     * @return true if the user is a member, false otherwise
     */
    boolean isMember(Long chatId, String userEmail);

    /**
     * Create an invitation for a user to join a chat.
     * This will also send a direct message to the invitee.
     *
     * @param chatId       the chat ID
     * @param inviterEmail the email address of the inviter
     * @param inviteeEmail the email address of the invitee
     * @return the created invitation DTO
     */
    ChatInvitationDTO createInvitation(Long chatId, String inviterEmail, String inviteeEmail);

    /**
     * Get all pending invitations for a user.
     *
     * @param userEmail the email address of the user
     * @return list of pending invitation DTOs
     */
    List<ChatInvitationDTO> getPendingInvitations(String userEmail);

    /**
     * Accept an invitation to join a chat.
     * This will send a direct message to the inviter.
     *
     * @param invitationId the invitation ID
     * @param userEmail    the email address of the accepting user
     */
    void acceptInvitation(Long invitationId, String userEmail);

    /**
     * Decline an invitation to join a chat.
     * This will send a direct message to the inviter.
     *
     * @param invitationId the invitation ID
     * @param userEmail    the email address of the declining user
     */
    void declineInvitation(Long invitationId, String userEmail);
}
