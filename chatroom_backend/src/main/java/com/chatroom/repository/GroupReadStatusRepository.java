package com.chatroom.repository;

import com.chatroom.entity.GroupReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GroupReadStatusRepository extends JpaRepository<GroupReadStatus, Long> {
    Optional<GroupReadStatus> findByUserIdAndGroupId(Long userId, Long groupId);
}
