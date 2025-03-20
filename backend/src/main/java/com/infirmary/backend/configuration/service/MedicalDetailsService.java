package com.infirmary.backend.configuration.service;
import java.util.Optional;

import com.infirmary.backend.configuration.model.MedicalDetails;

public interface MedicalDetailsService {
    Optional<MedicalDetails> getMedicalDetailsBySapEmail(String sapEmail);
}
