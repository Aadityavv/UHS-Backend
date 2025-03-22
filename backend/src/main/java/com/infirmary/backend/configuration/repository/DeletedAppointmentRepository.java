package com.infirmary.backend.configuration.repository;

import com.infirmary.backend.configuration.model.DeletedAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DeletedAppointmentRepository extends JpaRepository<DeletedAppointment, UUID> {
}
