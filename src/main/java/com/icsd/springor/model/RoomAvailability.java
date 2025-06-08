/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class RoomAvailability {
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek day;

    private LocalTime startTime;
    private LocalTime endTime;

    @Override
    public String toString() {
        return "RoomAvailability{" + "day=" + day + ", startTime=" + startTime + ", endTime=" + endTime + '}';
    }

    
}

