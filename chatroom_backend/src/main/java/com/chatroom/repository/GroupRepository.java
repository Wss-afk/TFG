package com.chatroom.repository;

import com.chatroom.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    // Buscar grupos donde el usuario con id dado sea miembro
    List<Group> findByUsers_Id(Long userId);
}