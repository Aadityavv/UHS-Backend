package com.infirmary.backend.configuration.dto;

import lombok.Data;

@Data
public class ActiveMedicationDTO {
    private String medicineName;
    private String dosage;
    private int duration;
    private String appointmentDate;
    private String endDate;
}
