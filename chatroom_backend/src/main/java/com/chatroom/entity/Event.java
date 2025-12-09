package com.chatroom.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date; // YYYY-MM-DD (día del evento)

    @Column(length = 80)
    private String time; // Ej: "07:00 - 10:00" o "Todo el día"

    @Column(length = 20)
    private String color; // color de marcador en calendario

    @Column(length = 255)
    private String description; // descripción opcional del evento

    // Relación real con el usuario creador
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    // Relación real con los responsables
    @ManyToMany
    @JoinTable(
        name = "event_responsibles",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> responsibles = new ArrayList<>();
}