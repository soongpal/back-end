package com.soongsil.soongpal.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soongsil.soongpal.chat.domain.ChatMessage;
import com.soongsil.soongpal.chat.domain.ChatRoom;
import com.soongsil.soongpal.chat.domain.ChatRoomUser;
import com.soongsil.soongpal.chat.domain.fcm.DeviceToken;
import com.soongsil.soongpal.chat.dto.ChatMessageReqDto;
import com.soongsil.soongpal.chat.dto.ChatMessageResDto;
import com.soongsil.soongpal.chat.dto.LastMessageDto;
import com.soongsil.soongpal.chat.repository.ChatMessageRepository;
import com.soongsil.soongpal.chat.repository.ChatRoomRepository;
import com.soongsil.soongpal.chat.repository.ChatRoomUserRepository;
import com.soongsil.soongpal.common.exception.ChatException;
import com.soongsil.soongpal.user.domain.User;
import com.soongsil.soongpal.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.soongsil.soongpal.common.exception.ChatErrorCode.*;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;
    private final UserRepository userRepository;
    private final FCMNotificationService fcmNotificationService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;


    public ChatMessageResDto saveMessage(Long roomId, ChatMessageReqDto dto, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_NOT_FOUND));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new ChatException(USER_NOT_FOUND));
        if (sender.getDeletedAt() != null) {
            throw new ChatException(USER_NOT_FOUND);
        }

        chatRoomUserRepository.findByChatRoomIdAndUserId(chatRoom.getId(), userId)
                .orElseThrow(() -> new ChatException(CHAT_ROOM_ACCESS_DENIED));

        ChatMessage chatMessage = ChatMessageReqDto.toEntity(dto, sender, chatRoom);
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);

        updateLastMessageCache(roomId, dto.getContent(), savedMessage);

        sendNotificationToOtherUsers(roomId, userId, sender.getNickName(), dto.getContent());
        Integer unreadCount = chatRoomUserRepository.countUnreadUsers(roomId, savedMessage.getId());
        return ChatMessageResDto.from(savedMessage, unreadCount);
    }

    private void updateLastMessageCache(Long roomId, String content, ChatMessage savedMessage) {
        try {
            LastMessageDto dto = new LastMessageDto(roomId, content, savedMessage.getCreatedAt());
            redisTemplate.opsForValue().set(
                    "chat:room:" + roomId + ":last-message",
                    objectMapper.writeValueAsString(dto)
            );
        } catch (Exception e) {
            log.warn("Redis cache update failed for room {}: {}", roomId, e.getMessage());
        }
    }

    private void sendNotificationToOtherUsers(Long roomId, Long senderId, String senderName, String message) {
        List<ChatRoomUser> otherUsers = chatRoomUserRepository.findByChatRoomIdAndUserIdNot(roomId, senderId);

        for (ChatRoomUser chatRoomUser : otherUsers) {
            User user = chatRoomUser.getUser();
            if (user.getDeviceTokens() != null && !user.getDeviceTokens().isEmpty() && user.getDeletedAt() == null) {
                for (DeviceToken token : user.getDeviceTokens()) {
                    if (token.isNotificationEnabled()) {
                        fcmNotificationService.sendChatNotification(
                                token.getToken(), senderName, message, roomId
                        );
                    }
                }
            }
        }
    }
}
