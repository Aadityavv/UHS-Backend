package com.infirmary.backend.configuration.controller;

import com.infirmary.backend.configuration.service.MedicalDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-details")
public class MedicalDetailsController {

    @Autowired
    private MedicalDetailsService medicalDetailsService;

    @GetMapping("/email/{sapEmail}")
    public ResponseEntity<?> getMedicalDetailsByEmail(@PathVariable String sapEmail) {
        return medicalDetailsService.getMedicalDetailsBySapEmail(sapEmail)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
