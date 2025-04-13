package com.jydoc.deliverable4.repositories.medicationrepositories;

import com.jydoc.deliverable4.model.MedicationIntakeLog;
import com.jydoc.deliverable4.model.MedicationModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationIntakeLogRepository extends JpaRepository<MedicationIntakeLog, Long> {
    List<MedicationIntakeLog> findByMedication(MedicationModel medication);

    void deleteByMedicationId(Long id);





}