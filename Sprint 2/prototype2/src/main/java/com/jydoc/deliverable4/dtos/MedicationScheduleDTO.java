package com.jydoc.deliverable4.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationScheduleDTO {

    public enum MedicationUrgency {
        URGENT, NONURGENT, ROUTINE
    }

    // Days of the week enum
    public enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    private Long medicationId;
    private String medicationName;
    private String dosage;
    private LocalTime scheduleTime;
    private boolean isTaken;
    private String instructions;
    private String status; // "UPCOMING", "MISSED", "TAKEN", etc.
    private MedicationUrgency urgency;
    private Set<DayOfWeek> daysOfWeek; // Days when medication should be taken

    public String getFormattedTime() {
        return scheduleTime != null ? scheduleTime.toString() : "";
    }

    public String getStatusBadgeClass() {
        return switch (status) {
            case "TAKEN" -> "badge bg-success";
            case "MISSED" -> "badge bg-danger";
            default -> "badge bg-warning text-dark";
        };
    }

    // Helper method to get days as comma-separated string
    public String getDaysAsString() {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return "Everyday";
        }
        return daysOfWeek.stream()
                .map(Enum::toString)
                .collect(Collectors.joining(", "));
    }
}