package com.infirmary.backend.configuration.controller;

import com.infirmary.backend.configuration.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    private final PrescriptionService prescriptionService;

    @Autowired
    public DiagnosisController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/frequencies")
    public ResponseEntity<Map<String, Integer>> getDiagnosisFrequencies() {
        Map<String, Integer> frequencies = prescriptionService.getDiagnosisFrequencies();
        return ResponseEntity.ok(frequencies);
    }
}
