package com.jydoc.deliverable4.model;

import jakarta.persistence.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Entity class representing a log entry for medication intake.
 */
@Entity
@Table(name = "medication_intake_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationIntakeLog {
    private static final Logger logger = LoggerFactory.getLogger(MedicationIntakeLog.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private MedicationModel medication;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "actual_intake_time")
    private LocalDateTime actualIntakeTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntakeStatus status;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt = LocalDateTime.now();

    public enum IntakeStatus {
        ON_TIME, LATE, MISSED, EARLY
    }



}