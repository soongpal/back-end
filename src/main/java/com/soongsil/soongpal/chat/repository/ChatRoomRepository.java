package com.soongsil.soongpal.chat.repository;

import com.soongsil.soongpal.chat.domain.ChatRoom;
import com.soongsil.soongpal.chat.dto.LastMessageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.chatRoomUsers cru WHERE cr.id = :roomId AND cru.user.id = :userId")
    Optional<ChatRoom> findChatRoomByIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.chatRoomUsers cru WHERE cru.user.id = :userId ORDER BY cr.updatedAt DESC")
    List<ChatRoom> findChatRoomsByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT
                cm.chat_room_id AS roomId,
                cm.content AS content,
                cm.created_at AS createdAt
            FROM chat_messages cm
            JOIN (
                SELECT chat_room_id, MAX(id) AS last_message_id
                FROM chat_messages
                WHERE chat_room_id IN (:roomIds)
                GROUP BY chat_room_id
            ) latest
            ON cm.id = latest.last_message_id
            """, nativeQuery = true)
    List<LastMessageProjection> findLastMessagesByRoomIds(@Param("roomIds") List<Long> roomIds);

    Optional<ChatRoom> findByBoardId(Long boardId);

    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN cr.chatRoomUsers cru1 " +
            "JOIN cr.chatRoomUsers cru2 " +
            "WHERE cr.type = 'PRIVATE' " +
            "AND cru1.user.id = :findUser " +
            "AND cru2.user.id = :boardUser " +
            "AND cru1.user.id <> cru2.user.id")
    Optional<ChatRoom> existsByTwoUser(@Param("findUser") Long findUser, @Param("boardUser") Long boardUserId);

}
