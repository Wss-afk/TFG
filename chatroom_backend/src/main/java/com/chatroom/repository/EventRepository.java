package com.chatroom.repository;

import com.chatroom.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByDateBetween(LocalDate start, LocalDate end);
    List<Event> findByCreatedBy_Id(Long userId);
    List<Event> findByResponsibles_Id(Long userId);
}