package com.infirmary.backend.configuration.impl;

import com.infirmary.backend.configuration.model.MedicalDetails;
import com.infirmary.backend.configuration.repository.MedicalDetailsRepository;
import com.infirmary.backend.configuration.service.MedicalDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MedicalDetailsServiceImpl implements MedicalDetailsService {

    @Autowired
    private MedicalDetailsRepository medicalDetailsRepository;

    @Override
    public Optional<MedicalDetails> getMedicalDetailsBySapEmail(String sapEmail) {
        return medicalDetailsRepository.findBySapEmail(sapEmail);
    }
}
