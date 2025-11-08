package com.chatroom.controller;

import com.chatroom.entity.Event;
import com.chatroom.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventRepository eventRepository;

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
    public ResponseEntity<Event> create(@RequestBody Event event) {
        try {
            if (event.getDate() == null || event.getTitle() == null) {
                return ResponseEntity.badRequest().build();
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