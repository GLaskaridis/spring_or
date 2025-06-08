package com.icsd.springor.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rooms")
@Getter
@Setter
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String building;

    @ElementCollection
    @CollectionTable(name = "room_availability", joinColumns = @JoinColumn(name = "room_id"))
    private Set<RoomAvailability> availability;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType type;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private boolean active = true;

    @Override
    public String toString() {
        return "Room{" + "id=" + id + ", name=" + name + ", building=" + building + ", availability=" + availability + ", capacity=" + capacity + ", type=" + type + ", location=" + location + ", active=" + active + '}';
    }

   

    
    
}

