package com.infirmary.backend.configuration.service;

import com.infirmary.backend.configuration.dto.PrescriptionReq;
import com.infirmary.backend.configuration.dto.ActiveMedicationDTO;


import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

public interface PrescriptionService {
    void submitPrescription(PrescriptionReq prescriptionDTO);
    Map<String, Integer> getDiagnosisFrequencies();
    ResponseEntity<?> getPrescription(UUID id);
    List<ActiveMedicationDTO> getActiveMedications(String sapEmail);
}
