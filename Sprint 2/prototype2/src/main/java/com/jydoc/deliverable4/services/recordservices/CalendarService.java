package com.jydoc.deliverable4.services.recordservices;

import com.jydoc.deliverable4.dtos.MedicationDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CalendarService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public String generateICalData(List<MedicationDTO> medications) {
        StringBuilder icalBuilder = new StringBuilder();

        // Calendar header
        icalBuilder.append("BEGIN:VCALENDAR\n")
                .append("VERSION:2.0\n")
                .append("PRODID:-//HealthTrack//Medication Schedule//EN\n")
                .append("METHOD:PUBLISH\n");

        // Add events for each medication
        medications.forEach(med -> {
            if (med.getIntakeTimes() != null && med.getDaysOfWeek() != null) {
                med.getIntakeTimes().forEach(time -> {
                    icalBuilder.append(createCalendarEvent(med, time));
                });
            }
        });

        icalBuilder.append("END:VCALENDAR");
        return icalBuilder.toString();
    }

    private String createCalendarEvent(MedicationDTO med, LocalTime time) {
        StringBuilder event = new StringBuilder();
        String uid = UUID.randomUUID().toString();
        LocalDate today = LocalDate.now();

        event.append("BEGIN:VEVENT\n")
                .append("UID:").append(uid).append("\n")
                .append("SUMMARY:").append(escapeText(med.getMedicationName())).append("\n")
                .append("DESCRIPTION:").append(escapeText(getDescription(med))).append("\n")
                .append("DTSTAMP:").append(today.atStartOfDay().format(DATE_TIME_FORMATTER)).append("\n")
                .append("DTSTART:").append(today.atTime(time).format(DATE_TIME_FORMATTER)).append("\n")
                .append("DTEND:").append(today.atTime(time.plusMinutes(5)).format(DATE_TIME_FORMATTER)).append("\n");

        if (!med.getDaysOfWeek().isEmpty()) {
            event.append("RRULE:FREQ=WEEKLY;BYDAY=")
                    .append(getDaysOfWeekAsICal(med.getDaysOfWeek()))
                    .append("\n");
        }

        event.append("END:VEVENT\n");
        return event.toString();
    }

    private String getDaysOfWeekAsICal(Set<MedicationDTO.DayOfWeek> days) {
        return days.stream()
                .map(day -> {
                    switch (day) {
                        case MONDAY: return "MO";
                        case TUESDAY: return "TU";
                        case WEDNESDAY: return "WE";
                        case THURSDAY: return "TH";
                        case FRIDAY: return "FR";
                        case SATURDAY: return "SA";
                        case SUNDAY: return "SU";
                        default: return "";
                    }
                })
                .collect(Collectors.joining(","));
    }

    private String getDescription(MedicationDTO med) {
        StringBuilder desc = new StringBuilder();
        desc.append("Take ").append(med.getMedicationName());
        if (med.getDosage() != null) {
            desc.append(", Dosage: ").append(med.getDosage());
        }
        if (med.getInstructions() != null) {
            desc.append(", Instructions: ").append(med.getInstructions());
        }
        return desc.toString();
    }

    private String escapeText(String text) {
        if (text == null) return "";
        return text.replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }
}