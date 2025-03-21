package com.infirmary.backend.configuration.recovery;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infirmary.backend.configuration.dto.AdminDTO;
import com.infirmary.backend.configuration.dto.LocationDataDTO;
import com.infirmary.backend.configuration.model.Admin;
import com.infirmary.backend.configuration.model.CurrentAppointment;
import com.infirmary.backend.configuration.model.Location;
import com.infirmary.backend.configuration.repository.AdminRepository;
import com.infirmary.backend.configuration.repository.CurrentAppointmentRepository;
import com.infirmary.backend.configuration.repository.LocationRepository;
import com.infirmary.backend.shared.utility.AppointmentQueueManager;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecoveryComponent {

    private final CurrentAppointmentRepository currentAppointmentRepository;
    private final LocationRepository locationRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void recover() {
        // Recover queues for appointments
        List<CurrentAppointment> cursApt = currentAppointmentRepository.findAllByAppointmentNotNullAndDoctorIsNull();

        for (CurrentAppointment crs : cursApt) {
            if (crs.getAppointment().getDoctor() == null)
                AppointmentQueueManager.addAppointmentToQueue(crs.getAppointment().getAppointmentId());

            if (crs.getAppointment().getDoctor() != null)
                AppointmentQueueManager.addAppointedQueue(crs.getAppointment().getAppointmentId());
        }

        ObjectMapper mapper = new ObjectMapper();

        // Recover locations if location table is empty
        if (!(locationRepository.count() > 0)) {
            try {
                // Load from resource stream
                ClassPathResource locationResource = new ClassPathResource("coords.json");
                InputStream locationStream = locationResource.getInputStream();

                TypeReference<List<LocationDataDTO>> typeReference = new TypeReference<>() {};
                List<LocationDataDTO> locations = mapper.readValue(locationStream, typeReference);

                for (LocationDataDTO location : locations) {
                    locationRepository.save(new Location(location));
                }

                System.out.println("✅ Locations recovered from coords.json: " + locations.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Recover admin accounts
        try {
            ClassPathResource adminResource = new ClassPathResource("admin.json");
            InputStream adminStream = adminResource.getInputStream();

            TypeReference<List<AdminDTO>> typeReference = new TypeReference<>() {};
            List<AdminDTO> admins = mapper.readValue(adminStream, typeReference);

            for (AdminDTO admin : admins) {
                if (adminRepository.existsByAdminEmail(admin.getEmail())) {
                    continue;
                }

                Admin adminEntity = new Admin();
                adminEntity.setAdminEmail(admin.getEmail());
                adminEntity.setName(admin.getName());
                adminEntity.setPassword(passwordEncoder.encode(admin.getPassword()));

                adminRepository.save(adminEntity);
            }

            System.out.println("✅ Admin accounts recovered from admin.json: " + admins.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
