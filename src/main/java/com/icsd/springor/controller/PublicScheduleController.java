package com.icsd.springor.controller;

import com.icsd.springor.model.CourseSchedule;
import com.icsd.springor.service.CourseScheduleService;
import com.icsd.springor.service.ExcelExportService;
import com.icsd.springor.service.ScheduleResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class PublicScheduleController {

    @Autowired
    private CourseScheduleService scheduleService;

    @Autowired
    private ScheduleResultService scheduleResultService;

    @Autowired
    private ExcelExportService excelExportService;

    //get /api/schedules/{id}/export/excel - εξαγωγή προγράμματος σε excel
    @GetMapping("/{id}/export/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROGRAM_MANAGER', 'TEACHER')")
    public ResponseEntity<byte[]> exportScheduleToExcel(@PathVariable Long id) {
        try {
            System.out.println("εξαγωγή προγράμματος " + id + " σε excel");
            
            //έλεγχος ότι υπάρχει ο χρονοπρογραμματισμός
            CourseSchedule schedule = scheduleService.getScheduleById(id);
            
            //έλεγχος ότι υπάρχουν αποτελέσματα
            if (!scheduleResultService.hasScheduleResults(id)) {
                System.out.println("δεν υπάρχουν αποτελέσματα για το πρόγραμμα " + id);
                return ResponseEntity.notFound().build();
            }
            
            //δημιουργία excel
            byte[] excelData = excelExportService.exportScheduleToExcel(id, schedule.getName());
            
            //ρύθμιση headers για download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "programma_" + schedule.getName().replaceAll(" ", "_") + ".xlsx");
            
            System.out.println("επιτυχής εξαγωγή προγράμματος " + id);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            System.out.println("σφάλμα εξαγωγής excel: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.out.println("σφάλμα: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    //public endpoints (χωρίς authentication)
    //get /api/schedules/public/latest - λήψη τελευταίου εγκεκριμένου προγράμματος
    @GetMapping("/public/latest")
    public ResponseEntity<Map<String, Object>> getLatestApprovedSchedule() {
        try {
            System.out.println("λήψη τελευταίου εγκεκριμένου προγράμματος (public)");
            
            //εύρεση τελευταίου εγκεκριμένου προγράμματος
            List<CourseSchedule> allSchedules = scheduleService.getAllSchedules();
            CourseSchedule latestApproved = allSchedules.stream()
                .filter(s -> s.getStatus() == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED)
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);
            
            if (latestApproved == null) {
                System.out.println("δεν υπάρχει εγκεκριμένο πρόγραμμα");
                return ResponseEntity.notFound().build();
            }
            
            //λήψη αποτελεσμάτων
            List<ScheduleResultService.ScheduleDisplayDTO> results = 
                scheduleResultService.getScheduleForDisplay(latestApproved.getId());
            
            Map<String, Object> response = Map.of(
                "scheduleId", latestApproved.getId(),
                "scheduleName", latestApproved.getName(),
                "semester", latestApproved.getSemester(),
                "results", results
            );
            
            System.out.println("βρέθηκε πρόγραμμα: " + latestApproved.getName() + 
                             " με " + results.size() + " αποτελέσματα");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("σφάλμα: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    //get /api/schedules/public/latest/export - εξαγωγή τελευταίου εγκεκριμένου προγράμματος σε excel
    @GetMapping("/public/latest/export")
    public ResponseEntity<byte[]> exportLatestApprovedScheduleToExcel() {
        try {
            System.out.println("εξαγωγή τελευταίου εγκεκριμένου προγράμματος σε excel (public)");
            
            //εύρεση τελευταίου εγκεκριμένου προγράμματος
            List<CourseSchedule> allSchedules = scheduleService.getAllSchedules();
            CourseSchedule latestApproved = allSchedules.stream()
                .filter(s -> s.getStatus() == CourseSchedule.ScheduleStatus.SOLUTION_APPROVED)
                .max((s1, s2) -> s1.getId().compareTo(s2.getId()))
                .orElse(null);
            
            if (latestApproved == null) {
                System.out.println("δεν υπάρχει εγκεκριμένο πρόγραμμα");
                return ResponseEntity.notFound().build();
            }
            
            //δημιουργία excel
            byte[] excelData = excelExportService.exportScheduleToExcel(
                latestApproved.getId(), 
                latestApproved.getName()
            );
            
            //ρύθμιση headers για download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", 
                "programma_" + latestApproved.getName().replaceAll(" ", "_") + ".xlsx");
            
            System.out.println("επιτυχής εξαγωγή προγράμματος: " + latestApproved.getName());
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
            
        } catch (IOException e) {
            System.out.println("σφάλμα εξαγωγής excel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            System.out.println("σφάλμα: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}