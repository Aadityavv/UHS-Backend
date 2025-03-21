package com.infirmary.backend.configuration.impl;

import com.infirmary.backend.configuration.model.Appointment;
import com.infirmary.backend.configuration.repository.AppointmentRepository;
import com.infirmary.backend.configuration.service.PostConstruct;
import com.infirmary.backend.shared.utility.AppointmentQueueManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueInitServiceImpl {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @PostConstruct
    public void initQueue() {
        List<Appointment> pendingAppointments = appointmentRepository.findAllPendingAppointments();

        for (Appointment appointment : pendingAppointments) {
            AppointmentQueueManager.addAppointmentToQueue(appointment.getAppointmentId());
        }

        System.out.println("Initialized Appointment Queue with " + pendingAppointments.size() + " appointments.");
    }
}
