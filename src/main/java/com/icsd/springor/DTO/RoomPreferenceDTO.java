/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomPreferenceDTO {
    private Long id;
    private Long assignmentId;
    private String courseName;
    private String courseCode;
    private Long roomId;
    private String roomName;
    private String roomBuilding;
    private Integer roomCapacity;
    private String roomType;
    private Integer preferenceWeight;
    private String notes;
    private boolean active;
}