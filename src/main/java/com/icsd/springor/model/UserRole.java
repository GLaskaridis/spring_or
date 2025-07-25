/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.icsd.springor.model;

public enum UserRole {
    TEACHER("ROLE_TEACHER", "Διδάσκων"),
    PROGRAM_MANAGER("ROLE_PROGRAM_MANAGER", "Επιμελητής Προγράμματος"), 
    ADMIN("ROLE_ADMIN", "Διαχειριστής");

    private final String roleName;
    private final String displayName;

    UserRole(String roleName, String displayName) {
        this.roleName = roleName;
        this.displayName = displayName;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
