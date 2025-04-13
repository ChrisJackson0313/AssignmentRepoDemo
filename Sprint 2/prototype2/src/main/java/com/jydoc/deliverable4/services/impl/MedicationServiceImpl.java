package com.jydoc.deliverable4.services.impl;

import com.jydoc.deliverable4.dtos.MedicationDTO;
import com.jydoc.deliverable4.dtos.MedicationScheduleDTO;
import com.jydoc.deliverable4.dtos.RefillReminderDTO;
import com.jydoc.deliverable4.model.*;
import com.jydoc.deliverable4.repositories.medicationrepositories.MedicationIntakeLogRepository;
import com.jydoc.deliverable4.repositories.medicationrepositories.MedicationIntakeTimeRepository;
import com.jydoc.deliverable4.repositories.medicationrepositories.MedicationRepository;
import com.jydoc.deliverable4.repositories.userrepositories.UserRepository;
import com.jydoc.deliverable4.security.Exceptions.*;
import com.jydoc.deliverable4.services.medicationservices.MedicationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the MedicationService interface providing medication management functionality.
 * This service handles CRUD operations for medications, schedule generation, and refill reminders.
 */
@Service
@RequiredArgsConstructor
public class MedicationServiceImpl implements MedicationService {

    private static final Logger logger = LoggerFactory.getLogger(MedicationServiceImpl.class);

    private final MedicationRepository medicationRepository;
    private final UserRepository userRepository;
    private final MedicationIntakeTimeRepository intakeTimeRepository;
    private final MedicationIntakeLogRepository intakeLogRepository;





    /**
     * Retrieves all medications for a specific user.
     *
     * @param username The username of the user whose medications to retrieve
     * @return List of MedicationDTO objects representing the user's medications
     * @throws UserNotFoundException if the specified user doesn't exist
     */
    @Override
    @Transactional(readOnly = true)
    public List<MedicationDTO> getUserMedications(String username) {
        logger.debug("Fetching medications for user: {}", username);
        long startTime = System.currentTimeMillis();

        try {
            validateUserExists(username);

            List<MedicationModel> medications = medicationRepository.findByUserUsername(username);
            logger.debug("Found {} medications in database query", medications.size());

            List<MedicationDTO> result = medications.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            logOperationSuccess("retrieved", result.size(), username, startTime);
            return result;
        } catch (Exception e) {
            logOperationError("fetching medications for user", username, e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MedicationDTO getMedicationById(Long id) {
        logger.debug("Fetching medication by ID: {}", id);

        try {
            MedicationModel medication = medicationRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Medication not found with ID: {}", id);
                        return new MedicationNotFoundException("Medication not found with ID: " + id);
                    });

            MedicationDTO result = convertToDto(medication);
            logger.info("Successfully retrieved medication ID: {}", id);
            return result;
        } catch (Exception e) {
            logOperationError("fetching medication by ID", id.toString(), e);
            throw e;
        }
    }

    /**
     * Retrieves a single medication by its ID.
     *
     * @param id The ID of the medication to retrieve
     * @return MedicationDTO representing the requested medication
     * @throws MedicationNotFoundException if no medication exists with the specified ID
     */
    @Override
    @Transactional(readOnly = true)
    public MedicationDTO getMedicationById(Long id, String username) {
        logger.debug("Fetching medication by ID: {} for user: {}", id, username);

        try {
            MedicationModel medication = medicationRepository.findByIdAndUserUsername(id, username)
                    .orElseThrow(() -> {
                        logger.error("Medication not found with ID: {} for user: {}", id, username);
                        return new MedicationNotFoundException("Medication not found with ID: " + id + " for user: " + username);
                    });

            MedicationDTO result = convertToDto(medication);
            logger.info("Successfully retrieved medication ID: {} for user: {}", id, username);
            return result;
        } catch (Exception e) {
            logOperationError("fetching medication by ID for user", id + " for " + username, e);
            throw e;
        }
    }

    /**
     * Creates a new medication for a user.
     *
     * @param medicationDTO DTO containing medication details
     * @param username The username of the user who will own this medication
     * @return MedicationDTO representing the created medication
     * @throws UserNotFoundException if the specified user doesn't exist
     */
    @Override
    @Transactional
    public MedicationDTO createMedication(MedicationDTO medicationDTO, String username) {
        logger.info("Creating new medication for user: {}", username);
        long startTime = System.currentTimeMillis();

        try {
            // Validate and get user
            UserModel user = validateAndGetUser(username);

            // Set default values if not provided
            if (medicationDTO.getActive() == null) {
                medicationDTO.setActive(true);
            }

            // Validate days of week
            if (medicationDTO.getDaysOfWeek() == null || medicationDTO.getDaysOfWeek().isEmpty()) {
                throw new IllegalArgumentException("At least one day of week must be selected");
            }

            // Build and convert the medication
            MedicationModel medication = convertToEntity(medicationDTO, user);

            // Process intake times
            processIntakeTimes(medicationDTO, medication);

            // Process days of week
            processDaysOfWeek(medicationDTO, medication);

            // Save the medication
            MedicationModel savedMedication = saveMedication(medication);

            // Convert to DTO and return
            MedicationDTO result = savedMedication.toDto();
            logOperationSuccess("created", savedMedication.getId(), username, startTime);
            return result;
        } catch (Exception e) {
            logOperationError("creating medication", username, e);
            throw new MedicationCreationException("Failed to create medication: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the days of medication intake for a specific medication.
     *
     * @param medicationId The ID of the medication to retrieve intake days for
     * @return Set of days of the week when the medication should be taken
     * @throws MedicationNotFoundException if no medication exists with the specified ID
     */
    @Override
    @Transactional(readOnly = true)
    public Set<MedicationDTO.DayOfWeek> getMedicationIntakeDays(Long medicationId) {
        logger.debug("Fetching intake days for medication ID: {}", medicationId);
        long startTime = System.currentTimeMillis();

        try {
            MedicationModel medication = medicationRepository.findById(medicationId)
                    .orElseThrow(() -> {
                        logger.error("Medication not found with ID: {}", medicationId);
                        return new MedicationNotFoundException("Medication not found with ID: " + medicationId);
                    });

            Set<MedicationDTO.DayOfWeek> days = medication.getDaysOfWeek().stream()
                    .map(day -> MedicationDTO.DayOfWeek.valueOf(day.name()))
                    .collect(Collectors.toSet());

            logger.info("Successfully retrieved {} intake days for medication ID {} in {} ms",
                    days.size(), medicationId, System.currentTimeMillis() - startTime);
            return days;
        } catch (Exception e) {
            logOperationError("fetching intake days for medication", medicationId.toString(), e);
            throw new MedicationNotFoundException("Failed to retrieve intake days for medication");
        }
    }


    /**
     * Updates an existing medication.
     *
     * @param id The ID of the medication to update
     * @param medicationDTO DTO containing updated medication details
     * @return MedicationDTO representing the updated medication
     * @throws MedicationNotFoundException if no medication exists with the specified ID
     */
    @Override
    @Transactional
    public MedicationDTO updateMedication(Long id, MedicationDTO medicationDTO) {
        // First validate the input parameters
        if (id == null) {
            logger.error("Attempted to update medication with null ID");
            throw new IllegalArgumentException("Medication ID cannot be null");
        }
        if (medicationDTO == null) {
            logger.error("Attempted to update with null medication DTO");
            throw new IllegalArgumentException("MedicationDTO cannot be null");
        }

        logger.info("Updating medication ID: {}", id);
        long startTime = System.currentTimeMillis();

        try {
            MedicationModel existing = getExistingMedication(id);
            updateMedicationFields(existing, medicationDTO);

            if (medicationDTO.getIntakeTimes() != null) {
                logger.debug("Processing {} intake times for update", medicationDTO.getIntakeTimes().size());
                updateIntakeTimes(existing, medicationDTO.getIntakeTimes());
            }

            if (medicationDTO.getDaysOfWeek() != null) {
                logger.debug("Processing {} days for update", medicationDTO.getDaysOfWeek().size());
                processDaysOfWeek(medicationDTO, existing);
            }

            MedicationModel updatedMedication = medicationRepository.save(existing);
            MedicationDTO result = convertToDto(updatedMedication);

            logOperationSuccess("updated", id, "medication", startTime);
            return result;
        } catch (Exception e) {
            logOperationError("updating medication", String.valueOf(id), e);  // Safe null handling
            throw new RuntimeException("Failed to update medication", e);
        }
    }

    /**
     * Deletes a medication.
     *
     * @param id The ID of the medication to delete
     * @throws MedicationNotFoundException if no medication exists with the specified ID
     */
    @Override
    @Transactional
    public void deleteMedication(Long id) {
        logger.info("Deleting medication ID: {}", id);
        long startTime = System.currentTimeMillis();

        try {
            // First get the medication with all relationships
            MedicationModel medication = medicationRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.error("Medication not found with ID: {}", id);
                        return new MedicationNotFoundException("Medication not found with ID: " + id);
                    });

            // Option 1: Delete all associated intake logs first
            intakeLogRepository.deleteByMedicationId(id);

            // Option 2: Alternatively, clear the collection if using orphanRemoval
            // medication.getIntakeLogs().clear();

            // Then delete the medication
            medicationRepository.delete(medication);

            logOperationSuccess("deleted", id, "medication", startTime);
        } catch (Exception e) {
            logOperationError("deleting medication", id.toString(), e);

            // More specific error handling
            if (e instanceof DataIntegrityViolationException) {
                throw new MedicationDeletionException(
                        "Cannot delete medication - it has associated records. Delete the records first.", e);
            } else if (e instanceof MedicationNotFoundException) {
                throw e; // Re-throw specific exceptions
            } else {
                throw new MedicationDeletionException("Failed to delete medication", e);
            }
        }
    }

    /**
     * Generates a medication schedule for a user.
     *
     * @param username The username of the user whose schedule to generate
     * @return List of MedicationScheduleDTO objects representing the schedule
     * @throws MedicationScheduleException if there's an error generating the schedule
     */


    @Override
    @Transactional(readOnly = true)
    public List<MedicationScheduleDTO> getMedicationSchedule(String username) {
        logger.debug("Fetching medication schedule for user: {}", username);
        long startTime = System.currentTimeMillis();

        try {
            // 1. Fetch medications with all necessary relationships
            List<MedicationModel> medications = medicationRepository.findByUserUsernameWithMedicationDetails(username);
            logger.debug("Found {} medications for user {}", medications.size(), username);

            if (medications.isEmpty()) {
                logger.info("No medications found for user {}", username);
                return Collections.emptyList();
            }

            // 2. Get current day of week
            DayOfWeek currentDay = LocalDate.now().getDayOfWeek();
            logger.debug("Current day of week: {}", currentDay);

            // 3. Convert to your model's DayOfWeek enum
            MedicationModel.DayOfWeek currentMedDay;
            try {
                currentMedDay = MedicationModel.DayOfWeek.valueOf(currentDay.name());
                logger.debug("Converted to model day: {}", currentMedDay);
            } catch (IllegalArgumentException e) {
                logger.error("Day of week conversion failed for {}", currentDay.name(), e);
                throw new MedicationScheduleException("Day of week conversion failed", e);
            }

            // 4. Process medications
            List<MedicationScheduleDTO> schedule = medications.stream()
                    .filter(Objects::nonNull)
                    .peek(med -> logger.trace("Processing medication: {}", med.getId()))
                    .filter(medication -> {
                        // Skip if no intake times
                        if (medication.getIntakeTimes() == null || medication.getIntakeTimes().isEmpty()) {
                            logger.debug("Medication {} skipped - no intake times", medication.getId());
                            return false;
                        }
                        return true;
                    })
                    .filter(medication -> {
                        // Include if no days specified OR current day matches
                        if (medication.getDaysOfWeek() == null || medication.getDaysOfWeek().isEmpty()) {
                            logger.trace("Medication {} included - no day restrictions", medication.getId());
                            return true;
                        }

                        boolean matchesDay = medication.getDaysOfWeek().contains(currentMedDay);
                        logger.trace("Medication {} day check: {}", medication.getId(), matchesDay);
                        return matchesDay;
                    })
                    .flatMap(this::processMedicationForSchedule)
                    .collect(Collectors.toList());

            logger.info("Generated schedule with {} entries for user {} in {} ms",
                    schedule.size(), username, System.currentTimeMillis() - startTime);

            return schedule;
        } catch (Exception e) {
            logger.error("Error generating schedule for user {}: {}", username, e.getMessage(), e);
            throw new MedicationScheduleException("Failed to generate medication schedule", e);
        }
    }

    /**
     * Retrieves upcoming medication refill reminders for a user.
     *
     * @param username The username of the user whose reminders to generate
     * @return List of RefillReminderDTO objects representing the reminders
     */
    @Override
    @Transactional(readOnly = true)
    public List<RefillReminderDTO> getUpcomingRefills(String username) {
        logger.debug("Fetching upcoming refills for user: {}", username);
        long startTime = System.currentTimeMillis();

        try {
            List<MedicationModel> medications = medicationRepository.findByUserUsername(username);
            logger.debug("Found {} medications for refill reminders", medications.size());

            List<RefillReminderDTO> reminders = medications.stream()
                    .map(this::mapToRefillReminderDTO)
                    .collect(Collectors.toList());

            logger.info("Generated {} refill reminders for user {} in {} ms",
                    reminders.size(), username, System.currentTimeMillis() - startTime);
            return reminders;
        } catch (Exception e) {
            logger.error("Error generating refill reminders for user {}: {}", username, e.getMessage(), e);
            throw e;
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validates that a user exists in the system.
     *
     * @param username The username to validate
     * @throws UserNotFoundException if the user doesn't exist
     */
    private void validateUserExists(String username) {
        if (!userRepository.existsByUsername(username)) {
            logger.error("User not found: {}", username);
            throw new UserNotFoundException(1L);
        }
    }

    /**
     * Validates and retrieves a user entity.
     *
     * @param username The username of the user to retrieve
     * @return UserModel entity
     * @throws UserNotFoundException if the user doesn't exist
     */
    private UserModel validateAndGetUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found: {}", username);
                    return new UserNotFoundException(1L);
                });
    }

    /**
     * Builds a MedicationDTO with user association.
     *
     * @param medicationDTO Source DTO with medication details
     * @param user The user who will own the medication
     * @return Prepared MedicationDTO
     */
    private MedicationDTO buildMedicationDTO(MedicationDTO medicationDTO, UserModel user) {
        return MedicationDTO.builder()
                .medicationName(medicationDTO.getMedicationName())
                .userId(user.getId())
                .urgency(medicationDTO.getUrgency())
                .dosage(medicationDTO.getDosage())
                .instructions(medicationDTO.getInstructions())
                .intakeTimes(medicationDTO.getIntakeTimes() != null ?
                        new ArrayList<>(medicationDTO.getIntakeTimes()) : new ArrayList<>())
                .daysOfWeek(medicationDTO.getDaysOfWeek() != null ?
                        new HashSet<>(medicationDTO.getDaysOfWeek()) : new HashSet<>())
                .active(medicationDTO.getActive() != null ?
                        medicationDTO.getActive() : true) // Default to true if null
                .build();
    }

    /**
     * Processes intake times for a medication during creation.
     *
     * @param medicationDTO DTO containing intake times
     * @param medication The medication entity to associate with intake times
     */
    private void processIntakeTimes(MedicationDTO medicationDTO, MedicationModel medication) {
        if (medicationDTO.getIntakeTimes() != null && !medicationDTO.getIntakeTimes().isEmpty()) {
            logger.debug("Processing {} intake times", medicationDTO.getIntakeTimes().size());
            medicationDTO.getIntakeTimes().forEach(time -> {
                MedicationIntakeTime mit = MedicationIntakeTime.builder()
                        .medication(medication)
                        .intakeTime(time)
                        .build();
                medication.addIntakeTime(mit.getIntakeTime());
            });
        }
    }

    /**
     * Saves a medication entity and validates the result.
     *
     * @param medication The medication to save
     * @return The saved medication entity
     * @throws RuntimeException if the save operation fails
     */
    private MedicationModel saveMedication(MedicationModel medication) {
        MedicationModel savedMedication = medicationRepository.saveAndFlush(medication);
        if (savedMedication.getId() == null) {
            logger.error("Saved medication has null ID!");
            throw new RuntimeException("Medication ID is null after save");
        }
        return savedMedication;
    }

    /**
     * Retrieves an existing medication entity.
     *
     * @param id The ID of the medication to retrieve
     * @return The medication entity
     * @throws MedicationNotFoundException if the medication doesn't exist
     */
    private MedicationModel getExistingMedication(Long id) {
        return medicationRepository.findById(id)
                .orElseThrow(() -> {
                    logger.error("Medication not found: {}", id);
                    return new MedicationNotFoundException("Medication not found with ID: " + id);
                });
    }

    /**
     * Validates that a medication exists.
     *
     * @param id The ID of the medication to validate
     * @throws MedicationNotFoundException if the medication doesn't exist
     */
    private void validateMedicationExists(Long id) {
        if (!medicationRepository.existsById(id)) {
            logger.error("Medication not found: {}", id);
            throw new MedicationNotFoundException("Medication not found with ID: " + id);
        }
    }

    /**
     * Deletes intake times associated with a medication.
     *
     * @param medicationId The ID of the medication whose intake times to delete
     */
    private void deleteIntakeTimes(Long medicationId) {
        int deletedIntakeTimes = intakeTimeRepository.deleteByMedicationId(medicationId);
        logger.debug("Deleted {} intake times", deletedIntakeTimes);
    }

    /**
     * Processes a medication for schedule generation.
     *
     * @param medication The medication to process
     * @return Stream of MedicationScheduleDTO objects
     */
    private Stream<MedicationScheduleDTO> processMedicationForSchedule(MedicationModel medication) {
        logger.trace("Processing medication ID: {}", medication.getId());
        return medication.getIntakeTimes().stream()
                .filter(Objects::nonNull)
                .map(intakeTimeEntity -> createScheduleDTO(medication, intakeTimeEntity));
    }

    /**
     * Creates a schedule DTO from medication and intake time.
     *
     * @param medication The medication entity
     * @param intakeTimeEntity The intake time entity
     * @return MedicationScheduleDTO
     */
    private MedicationScheduleDTO createScheduleDTO(MedicationModel medication, MedicationIntakeTime intakeTimeEntity) {
        if (intakeTimeEntity == null) {
            throw new IllegalArgumentException("Intake time entity cannot be null");
        }
        return createScheduleDTO(medication, intakeTimeEntity.getIntakeTime());
    }

    /**
     * Creates a schedule DTO from medication and time.
     *
     * @param medication The medication entity
     * @param intakeTime The intake time
     * @return MedicationScheduleDTO
     */
    private MedicationScheduleDTO createScheduleDTO(MedicationModel medication, LocalTime intakeTime) {
        if (intakeTime == null) {
            logger.warn("Null intake time encountered for medication ID: {}", medication.getId());
            intakeTime = LocalTime.MIDNIGHT;
        }

        return MedicationScheduleDTO.builder()
                .medicationId(medication.getId())
                .medicationName(medication.getName())
                .dosage(medication.getDosage())
                .scheduleTime(intakeTime)
                .isTaken(false)
                .instructions(medication.getInstructions())
                .status("UPCOMING")
                .urgency(convertToDtoUrgency(medication.getUrgency()))
                .build();
    }

    /**
     * Converts medication urgency to DTO format.
     *
     * @param modelUrgency The urgency from the model
     * @return Converted urgency for DTO
     */
    private MedicationScheduleDTO.MedicationUrgency convertToDtoUrgency(MedicationModel.MedicationUrgency modelUrgency) {
        if (modelUrgency == null) {
            logger.debug("Null urgency encountered, defaulting to ROUTINE");
            return MedicationScheduleDTO.MedicationUrgency.ROUTINE;
        }
        try {
            return MedicationScheduleDTO.MedicationUrgency.valueOf(modelUrgency.name());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown urgency value: {}, defaulting to ROUTINE", modelUrgency);
            return MedicationScheduleDTO.MedicationUrgency.ROUTINE;
        }
    }

    /**
     * Updates intake times for a medication.
     *
     * @param medication The medication to update
     * @param newIntakeTimes The new intake times to set
     */
    private void updateIntakeTimes(MedicationModel medication, List<LocalTime> newIntakeTimes) {
        Set<LocalTime> currentTimes = medication.getIntakeTimesAsLocalTimes();
        currentTimes.stream()
                .filter(time -> !newIntakeTimes.contains(time))
                .forEach(medication::removeIntakeTime);
        newIntakeTimes.stream()
                .filter(time -> !currentTimes.contains(time))
                .forEach(medication::addIntakeTime);
    }

    /**
     * Updates medication fields from DTO.
     *
     * @param medication The medication to update
     * @param dto The DTO containing new values
     */
    private void updateMedicationFields(MedicationModel medication, MedicationDTO dto) {
        logger.trace("Updating fields for medication ID: {}", medication.getId());

        if (dto.getMedicationName() != null) {
            medication.setName(dto.getMedicationName());
        } else {
            logger.warn("Medication name is null in DTO for medication ID: {}", medication.getId());
        }

        if (dto.getUrgency() != null) {
            try {
                medication.setUrgency(MedicationModel.MedicationUrgency.valueOf(dto.getUrgency().name()));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid urgency value in DTO: {}. Keeping existing value for medication ID: {}",
                        dto.getUrgency(), medication.getId());
            }
        } else {
            logger.warn("Urgency is null in DTO for medication ID: {}", medication.getId());
        }

        medication.setDosage(dto.getDosage());
        medication.setInstructions(dto.getInstructions());

        if (dto.getDaysOfWeek() != null) {
            processDaysOfWeek(dto, medication);
        }


    }

    /**
     * Converts a medication entity to DTO.
     *
     * @param medication The medication to convert
     * @return MedicationDTO
     */
    private MedicationDTO convertToDto(MedicationModel medication) {
        Set<LocalTime> intakeTimesSet = medication.getIntakeTimesAsLocalTimes();
        List<LocalTime> intakeTimes = intakeTimesSet != null ? new ArrayList<>(intakeTimesSet) : new ArrayList<>();
        return MedicationDTO.builder()
                .id(medication.getId()) // Sets id from entity
                .userId(medication.getUser() != null ? medication.getUser().getId() : null)
                .medicationName(medication.getName())
                .urgency(medication.getUrgency() != null ?
                        MedicationDTO.MedicationUrgency.valueOf(medication.getUrgency().name()) : null)
                .dosage(medication.getDosage())
                .instructions(medication.getInstructions())
                .intakeTimes(intakeTimes)
                .daysOfWeek(medication.getDaysOfWeek() != null && !medication.getDaysOfWeek().isEmpty() ?
                        medication.getDaysOfWeek().stream()
                                .map(day -> MedicationDTO.DayOfWeek.valueOf(day.name()))
                                .collect(Collectors.toSet()) : null)
                .build();
    }

    /**
     * Converts a medication DTO to entity.
     *
     * @param dto The DTO to convert
     * @param user The user who will own the medication
     * @return MedicationModel
     */
    private MedicationModel convertToEntity(MedicationDTO dto, UserModel user) {
        logger.trace("Converting DTO to medication entity for user ID: {}", user.getId());

        return MedicationModel.builder()
                .user(user)
                .name(dto.getMedicationName())
                .urgency(MedicationModel.MedicationUrgency.valueOf(dto.getUrgency().name()))
                .dosage(dto.getDosage())
                .instructions(dto.getInstructions())
                .build();
    }

    /**
     * Maps a medication to a refill reminder DTO.
     *
     * @param medication The medication to map
     * @return RefillReminderDTO
     */
    private RefillReminderDTO mapToRefillReminderDTO(MedicationModel medication) {
        logger.trace("Mapping medication ID {} to refill reminder", medication.getId());

        int remainingDoses = calculateRemainingDoses(medication);
        logger.trace("Calculated {} remaining doses", remainingDoses);

        return RefillReminderDTO.builder()
                .medicationId(medication.getId())
                .medicationName(medication.getName())
                .remainingDoses(remainingDoses)
                .refillByDate(LocalDate.now().plusDays(7))
                .urgency(medication.getUrgency().name())
                .build();
    }

    /**
     * Calculates remaining doses for a medication.
     *
     * @param medication The medication to calculate for
     * @return Estimated number of remaining doses
     */
    private int calculateRemainingDoses(MedicationModel medication) {
        int count = medication.getIntakeTimes().size() * 7; // Assume 1 week supply
        logger.trace("Calculated remaining doses: {} ({} intake times × 7 days)",
                count, medication.getIntakeTimes().size());
        return count;
    }

    /**
     * Logs a successful operation.
     *
     * @param operation Description of the operation performed
     * @param identifier Identifier of the affected entity
     * @param entityType Type of entity affected
     * @param startTime Start time of the operation (for duration calculation)
     */
    private void logOperationSuccess(String operation, Object identifier, String entityType, long startTime) {
        logger.info("Successfully {} {} {} in {} ms",
                operation, entityType, identifier, System.currentTimeMillis() - startTime);
    }

    /**
     * Logs an operation error.
     *
     * @param operation Description of the operation attempted
     * @param identifier Identifier of the affected entity
     * @param exception The exception that occurred
     */
    private void logOperationError(String operation, String identifier, Exception exception) {
        logger.error("Error {} {}: {}", operation, identifier, exception.getMessage(), exception);
    }



    private void processDaysOfWeek(MedicationDTO medicationDTO, MedicationModel medication) {
        if (medicationDTO.getDaysOfWeek() == null || medicationDTO.getDaysOfWeek().isEmpty()) {
            logger.warn("No days selected for medication ID: {}", medication.getId());
            medication.clearDays();
            return;
        }

        medication.clearDays();
        medicationDTO.getDaysOfWeek().forEach(day -> {
            try {
                medication.addDay(MedicationModel.DayOfWeek.valueOf(day.name()));
            } catch (IllegalArgumentException e) {
                logger.error("Invalid day value: {}", day);
            }
        });
    }

    /**
     * Logs a medication intake.
     *
     * @param medicationId The ID of the medication
     * @param scheduledTime The scheduled intake time
     * @param actualTime The actual intake time
     * @param username The username of the user
     */
    @Override
    @Transactional
    public void logMedicationIntake(Long medicationId, LocalDateTime scheduledTime,
                                    LocalDateTime actualTime, String username) {

        try {
            // 1. Validate input times
            if (scheduledTime == null) {
                throw new RuntimeException();
            }

            // 2. Fetch medication
            MedicationModel medication = medicationRepository.findByIdAndUserUsername(medicationId, username)
                    .orElseThrow(() -> new MedicationNotFoundException("Medication not found for user"));

            // 3. Determine status with proper timezone-aware comparison
            MedicationIntakeLog.IntakeStatus status = determineIntakeStatus(scheduledTime, actualTime);

            // 4. Create and save log
            MedicationIntakeLog log = new MedicationIntakeLog();
            log.setMedication(medication);
            log.setScheduledTime(scheduledTime);
            log.setActualIntakeTime(actualTime);
            log.setStatus(status);
            log.setLoggedAt(LocalDateTime.now());

            intakeLogRepository.save(log);

        } catch (MedicationNotFoundException e) {
            logger.error("Medication not found: ID={}, User={}", medicationId, username, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to log intake for medication ID: {}", medicationId, e);
            throw new RuntimeException(e);
        }
    }

    private MedicationIntakeLog.IntakeStatus determineIntakeStatus(LocalDateTime scheduledTime,
                                                                   LocalDateTime actualTime) {
        if (actualTime == null) {
            return MedicationIntakeLog.IntakeStatus.MISSED;
        }

        // Convert to Instant for timezone-agnostic comparison
        Instant scheduledInstant = scheduledTime.atZone(ZoneId.systemDefault()).toInstant();
        Instant actualInstant = actualTime.atZone(ZoneId.systemDefault()).toInstant();

        long minutesDifference = ChronoUnit.MINUTES.between(scheduledInstant, actualInstant);

        if (minutesDifference >= -15 && minutesDifference <= 15) {  // ±15 minute window
            return MedicationIntakeLog.IntakeStatus.ON_TIME;
        } else if (minutesDifference > 15) {
            return MedicationIntakeLog.IntakeStatus.LATE;
        } else {
            return MedicationIntakeLog.IntakeStatus.EARLY;
        }
    }



    /**
     * Retrieves intake statistics for a medication.
     *
     * @param medicationId The ID of the medication
     * @param username The username of the user
     * @return Map containing stats (onTime, late, missed counts)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getMedicationIntakeStats(Long medicationId, String username) {
        MedicationModel medication = medicationRepository.findByIdAndUserUsername(medicationId, username)
                .orElseThrow(() -> new MedicationNotFoundException("Medication not found"));

        List<MedicationIntakeLog> logs = intakeLogRepository.findByMedication(medication);

        return logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getStatus().name(),
                        Collectors.counting()
                ));
    }










}