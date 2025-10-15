package com.chatroom.repository;

import com.chatroom.entity.Message;
import com.chatroom.entity.User;
import com.chatroom.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByReceiver(User receiver);

    List<Message> findByGroup(Group group);

    @Query("SELECT m FROM Message m WHERE (m.group.id = :groupId OR m.receiver.id = :receiverId)")
    List<Message> findByGroupIdOrReceiverId(@Param("groupId") Long groupId, @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Message m WHERE (m.sender.id = :userId AND m.receiver.id = :receiverId) OR (m.sender.id = :receiverId AND m.receiver.id = :userId)")
    List<Message> findChatHistory(@Param("userId") Long userId, @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Message m WHERE m.receiver.id = :userId AND m.sender.id = :senderId AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("userId") Long userId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.sender.id = :senderId AND m.isRead = false")
    int countUnreadMessages(@Param("userId") Long userId, @Param("senderId") Long senderId);
}