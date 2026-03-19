/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.chat;

import java.util.List;

/**
 * Chat Service Interface
 *
 * @author info@plaintext.ch
 * @since 2026
 */
public interface ChatService {

    /**
     * Create a new chat
     */
    ChatDTO createChat(String chatName, String owner, String mandat);

    /**
     * Add a member to a chat
     */
    void addMember(Long chatId, String userEmail);

    /**
     * Remove a member from a chat
     */
    void removeMember(Long chatId, String userEmail);

    /**
     * Send a message in a chat
     */
    ChatMessageDTO sendMessage(Long chatId, String senderEmail, String messageText);

    /**
     * Get all chats owned by a user
     */
    List<ChatDTO> getOwnedChats(String owner, String mandat);

    /**
     * Get all chats where user is a member
     */
    List<ChatDTO> getChatsByMember(String userEmail, String mandat);

    /**
     * Get all messages in a chat
     */
    List<ChatMessageDTO> getMessages(Long chatId);

    /**
     * Get messages in a chat with pagination support
     * @param chatId The chat ID
     * @param page The page number (0-based)
     * @param size The page size (number of messages per page)
     * @return ChatMessagesPageDTO containing messages and pagination info
     */
    ChatMessagesPageDTO getMessages(Long chatId, int page, int size);

    /**
     * Get a chat by ID
     */
    ChatDTO getChat(Long chatId);

    /**
     * Delete a chat
     */
    void deleteChat(Long chatId);

    /**
     * Check if user is member of chat
     */
    boolean isMember(Long chatId, String userEmail);

    /**
     * Create invitation for a user to join a chat
     * This will also send a direct message to the invitee
     */
    ChatInvitationDTO createInvitation(Long chatId, String inviterEmail, String inviteeEmail);

    /**
     * Get all pending invitations for a user
     */
    List<ChatInvitationDTO> getPendingInvitations(String userEmail);

    /**
     * Accept an invitation
     * This will send a direct message to the inviter
     */
    void acceptInvitation(Long invitationId, String userEmail);

    /**
     * Decline an invitation
     * This will send a direct message to the inviter
     */
    void declineInvitation(Long invitationId, String userEmail);
}
