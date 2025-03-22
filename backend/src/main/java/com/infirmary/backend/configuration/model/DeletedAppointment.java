package com.infirmary.backend.configuration.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletedAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private UUID appointmentId;
    private String patientName;
    private String patientEmail;
    private String reason;
    private LocalDateTime deletedAt;

    private String deletedBy; // AD email, optional
}
