/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum TeacherType {
    @JsonProperty("ΔΕΠ")
    DEP,
    
    @JsonProperty("ΕΔΙΠ")
    EDIP,
    
    @JsonProperty("ΕΤΕΠ")
    ETEP,
    
    @JsonProperty("Άλλο")
    OTHER
}