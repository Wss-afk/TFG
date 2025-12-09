package com.chatroom.controller;

import com.chatroom.entity.Event;
import com.chatroom.entity.User;
import com.chatroom.repository.EventRepository;
import com.chatroom.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;

    @Data
    static class EventRequest {
        private String title;
        private LocalDate date;
        private String time;
        private String color;
        private String description;
        private Long createdById;
        private List<Long> responsibleIds;
    }

    /**
     * Lista eventos del mes indicado (year, month 1-12).
     * GET /api/events/list?year=2025&month=11
     */
    @GetMapping("/list")
    public ResponseEntity<List<Event>> listMonth(@RequestParam int year, @RequestParam int month) {
        try {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            List<Event> events = eventRepository.findByDateBetween(start, end);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of());
        }
    }

    /**
     * Crea un evento sencillo.
     */
    @PostMapping
    public ResponseEntity<Event> create(@RequestBody EventRequest req) {
        try {
            if (req.getDate() == null || req.getTitle() == null) {
                return ResponseEntity.badRequest().build();
            }
            Event event = new Event();
            event.setTitle(req.getTitle());
            event.setDate(req.getDate());
            event.setTime(req.getTime());
            event.setColor(req.getColor());
            event.setDescription(req.getDescription());

            if (req.getCreatedById() != null) {
                userRepository.findById(req.getCreatedById()).ifPresent(event::setCreatedBy);
            }
            if (req.getResponsibleIds() != null && !req.getResponsibleIds().isEmpty()) {
                List<User> responsibles = userRepository.findAllById(req.getResponsibleIds());
                event.setResponsibles(responsibles);
            }

            Event saved = eventRepository.save(event);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            eventRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}