package com.icsd.springor.service;

import com.icsd.springor.model.ScheduleResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    @Autowired
    private ScheduleResultService scheduleResultService;

    //δημιουργία excel αρχείου με το πρόγραμμα οργανωμένο ανά εξάμηνο
    public byte[] exportScheduleToExcel(Long scheduleId, String scheduleName) throws IOException {
        List<ScheduleResult> results = scheduleResultService.getScheduleResultsWithDetails(scheduleId);

        try (Workbook workbook = new XSSFWorkbook()) {
            //ομαδοποίηση ανά εξάμηνο
            Map<Integer, List<ScheduleResult>> resultsBySemester = results.stream()
                .collect(Collectors.groupingBy(r -> r.getAssignment().getCourse().getSemester()));

            //ταξινόμηση εξαμήνων
            List<Integer> semesters = new ArrayList<>(resultsBySemester.keySet());
            Collections.sort(semesters);

            //δημιουργία styles (μία φορά για όλα τα sheets)
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle cellStyle = createCellStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle timeSlotStyle = createTimeSlotStyle(workbook);

            //δημιουργία ενός sheet ανά εξάμηνο
            for (Integer semester : semesters) {
                List<ScheduleResult> semesterResults = resultsBySemester.get(semester);
                createSemesterSheet(workbook, semester, semesterResults, scheduleName, 
                                  headerStyle, cellStyle, titleStyle, timeSlotStyle);
            }

            //μετατροπή σε byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    //δημιουργία sheet για συγκεκριμένο εξάμηνο
    private void createSemesterSheet(Workbook workbook, Integer semester, 
                                     List<ScheduleResult> results, String scheduleName,
                                     CellStyle headerStyle, CellStyle cellStyle, 
                                     CellStyle titleStyle, CellStyle timeSlotStyle) {
        //δημιουργία sheet με όνομα "1ο Εξάμηνο", "2ο Εξάμηνο" κλπ
        String sheetName = semester + "ο Εξάμηνο";
        Sheet sheet = workbook.createSheet(sheetName);

        int rowNum = 0;

        //τίτλος
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(scheduleName + " - " + sheetName);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        //κενή γραμμή
        rowNum++;

        //οργάνωση δεδομένων ανά ημέρα και slot
        Map<DayOfWeek, Map<Integer, List<ScheduleResult>>> scheduleByDayAndSlot = 
            organizeSchedule(results);

        //δημιουργία headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Ώρα", "Δευτέρα", "Τρίτη", "Τετάρτη", "Πέμπτη", "Παρασκευή"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        //ορισμός χρονικών διαστημάτων
        String[] timeSlots = {"08:00 - 10:00", "10:00 - 12:00", "12:00 - 14:00", "14:00 - 16:00"};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                           DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        //δημιουργία γραμμών ανά χρονικό διάστημα
        for (int slot = 0; slot < 4; slot++) {
            Row row = sheet.createRow(rowNum++);
            
            //στήλη ώρας
            Cell timeCell = row.createCell(0);
            timeCell.setCellValue(timeSlots[slot]);
            timeCell.setCellStyle(timeSlotStyle);

            //στήλες για κάθε ημέρα
            for (int dayIndex = 0; dayIndex < days.length; dayIndex++) {
                DayOfWeek day = days[dayIndex];
                Cell dayCell = row.createCell(dayIndex + 1);
                
                //βρίσκουμε τα μαθήματα για αυτή την ημέρα και το slot
                Map<Integer, List<ScheduleResult>> slotsForDay = scheduleByDayAndSlot.get(day);
                String cellContent = "";
                
                if (slotsForDay != null) {
                    List<ScheduleResult> coursesInSlot = slotsForDay.get(slot);
                    if (coursesInSlot != null && !coursesInSlot.isEmpty()) {
                        //δημιουργία περιεχομένου κελιού με όλα τα μαθήματα
                        cellContent = coursesInSlot.stream()
                            .map(r -> formatCourseForCell(r))
                            .collect(Collectors.joining("\n\n"));
                    }
                }
                
                dayCell.setCellValue(cellContent);
                dayCell.setCellStyle(cellStyle);
            }
        }

        //αυτόματη ρύθμιση πλάτους στηλών
        sheet.setColumnWidth(0, 4000); //στήλη ώρας
        for (int i = 1; i <= 5; i++) {
            sheet.setColumnWidth(i, 8000); //στήλες ημερών
        }
    }

    //μορφοποίηση μαθήματος για εμφάνιση στο κελί
    private String formatCourseForCell(ScheduleResult result) {
        StringBuilder sb = new StringBuilder();
        
        //όνομα μαθήματος
        sb.append(result.getAssignment().getCourse().getName());
        sb.append("\n");
        
        //κωδικός - συνιστώσα
        sb.append(result.getAssignment().getCourse().getCode());
        sb.append(" - ");
        sb.append(getComponentInGreek(result.getAssignment().getCourseComponent()));
        sb.append("\n");
        
        //καθηγητής
        sb.append("Καθηγητής: ");
        sb.append(result.getAssignment().getTeacher().getFullName());
        sb.append("\n");
        
        //αίθουσα
        sb.append("Αίθουσα: ");
        sb.append(result.getRoom().getName());
        
        return sb.toString();
    }

    //οργάνωση αποτελεσμάτων ανά ημέρα και slot
    private Map<DayOfWeek, Map<Integer, List<ScheduleResult>>> organizeSchedule(List<ScheduleResult> results) {
        Map<DayOfWeek, Map<Integer, List<ScheduleResult>>> organized = new HashMap<>();
        
        for (ScheduleResult result : results) {
            organized.computeIfAbsent(result.getDayOfWeek(), k -> new HashMap<>())
                    .computeIfAbsent(result.getSlotNumber(), k -> new ArrayList<>())
                    .add(result);
        }
        
        return organized;
    }

    //δημιουργία style για header
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    //δημιουργία style για κελιά
    private CellStyle createCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    //δημιουργία style για τίτλο
    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    //δημιουργία style για time slots
    private CellStyle createTimeSlotStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    //μετάφραση συνιστώσας σε ελληνικά
    private String getComponentInGreek(com.icsd.springor.model.Course.TeachingHours.CourseComponent component) {
        switch (component) {
            case THEORY: return "Θεωρία";
            case LABORATORY: return "Εργαστήριο";
            default: return component.toString();
        }
    }
}