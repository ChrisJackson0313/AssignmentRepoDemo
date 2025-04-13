package com.jydoc.deliverable4.dtos;

import lombok.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data Transfer Object (DTO) representing medication information in the system.
 * This class now includes support for medication intake statistics tracking.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicationDTO {

    private Long id;

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotBlank(message = "Medication name cannot be blank")
    private String medicationName;

    @NotNull(message = "Urgency level must be specified")
    private MedicationUrgency urgency;

    @Builder.Default
    @NotNull(message = "Intake times must be specified")
    private List<LocalTime> intakeTimes = new ArrayList<>();

    @NotNull(message = "Days of week must be specified")
    private Set<DayOfWeek> daysOfWeek;

    private String dosage;
    private String instructions;
    private Boolean active;

    // New field for intake statistics
    @Builder.Default
    private Map<String, Long> stats = new HashMap<>();

    /**
     * Enumeration representing the urgency level of a medication.
     */
    public enum MedicationUrgency {
        URGENT,
        NONURGENT,
        ROUTINE;

        public static MedicationUrgency fromString(String value) {
            if (value == null) {
                return ROUTINE;
            }
            try {
                return MedicationUrgency.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid urgency value. Must be 'URGENT', 'NONURGENT' or 'ROUTINE'");
            }
        }
    }

    /**
     * Enumeration representing days of the week with display names.
     */
    @Getter
    public enum DayOfWeek {
        MONDAY("Monday"),
        TUESDAY("Tuesday"),
        WEDNESDAY("Wednesday"),
        THURSDAY("Thursday"),
        FRIDAY("Friday"),
        SATURDAY("Saturday"),
        SUNDAY("Sunday");

        private final String displayName;

        DayOfWeek(String displayName) {
            this.displayName = displayName;
        }

        public static DayOfWeek fromDayOfMonth(Integer day, Integer year, Integer month) {
            try {
                LocalDate date = LocalDate.of(year, month, day);
                java.time.DayOfWeek javaDayOfWeek = date.getDayOfWeek();
                return DayOfWeek.valueOf(javaDayOfWeek.name());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid date parameters: day=" + day + ", year=" + year + ", month=" + month, e);
            }
        }
    }

    /**
     * Gets intake times as a comma-separated string for JavaScript processing.
     */
    public String getIntakeTimesAsString() {
        if (intakeTimes == null || intakeTimes.isEmpty()) {
            return "";
        }
        return intakeTimes.stream()
                .map(time -> String.format("%02d:%02d", time.getHour(), time.getMinute()))
                .collect(Collectors.joining(","));
    }

    /**
     * Determines if the medication is currently active.
     */
    public boolean isActive() {
        return active != null && active;
    }

    /**
     * Adds or updates a statistic count.
     * @param key The statistic key (e.g., "ON_TIME", "LATE")
     * @param value The count to add
     */
    public void addStatistic(String key, long value) {
        stats.merge(key, value, Long::sum);
    }

    /**
     * Gets a specific statistic count.
     * @param key The statistic key
     * @return The count, or 0 if not found
     */
    public long getStatistic(String key) {
        return stats.getOrDefault(key, 0L);
    }

    /**
     * Gets all statistics as an unmodifiable map.
     */
    public Map<String, Long> getStats() {
        return Collections.unmodifiableMap(stats);
    }

    /**
     * Sets all statistics at once.
     */
    public void setStats(Map<String, Long> stats) {
        this.stats = new HashMap<>(stats);
    }
}