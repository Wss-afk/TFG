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
    private String price; // Ej: "$5.0" (opcional)

    @Column(length = 20)
    private String color; // color de marcador en calendario

    @Column(length = 255)
    private String description; // descripción opcional del evento

    // Nuevo: identificador del creador del evento (usuario)
    @Column(name = "created_by_id")
    private Long createdById;

    // Nuevo: lista de responsables (ids de usuario) usando tabla intermedia sencilla
    @ElementCollection
    @CollectionTable(name = "event_responsible_ids", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "user_id")
    private List<Long> responsibleIds = new ArrayList<>();
}