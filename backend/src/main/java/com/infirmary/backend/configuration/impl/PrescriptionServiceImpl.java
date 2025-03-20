package com.infirmary.backend.configuration.impl;

import com.infirmary.backend.configuration.dto.PrescriptionDTO;
import com.infirmary.backend.configuration.dto.PrescriptionMedsDTO;
import com.infirmary.backend.configuration.dto.PrescriptionReq;
import com.infirmary.backend.configuration.dto.PrescriptionRes;
import com.infirmary.backend.configuration.model.Appointment;
import com.infirmary.backend.configuration.model.CurrentAppointment;
import com.infirmary.backend.configuration.model.Doctor;
import com.infirmary.backend.configuration.model.Prescription;
import com.infirmary.backend.configuration.model.PrescriptionMeds;
import com.infirmary.backend.configuration.model.Stock;
import com.infirmary.backend.configuration.repository.AppointmentRepository;
import com.infirmary.backend.configuration.dto.ActiveMedicationDTO;
import com.infirmary.backend.configuration.repository.CurrentAppointmentRepository;
import com.infirmary.backend.configuration.repository.DoctorRepository;
import com.infirmary.backend.configuration.repository.MedicalDetailsRepository;
import com.infirmary.backend.configuration.repository.PrescriptionMedsRepository;
import com.infirmary.backend.configuration.repository.PrescriptionRepository;
import com.infirmary.backend.configuration.repository.StockRepository;
import com.infirmary.backend.configuration.service.PrescriptionService;
import com.infirmary.backend.shared.utility.AppointmentQueueManager;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CurrentAppointmentRepository currentAppointmentRepository;
    private final AppointmentRepository appointmentRepository;
    private final StockRepository stockRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalDetailsRepository medicalDetailsRepository;
    private final PrescriptionMedsRepository prescriptionMedsRepository;

    @Autowired
    public PrescriptionServiceImpl(
            PrescriptionRepository prescriptionRepository,
            CurrentAppointmentRepository currentAppointmentRepository,
            AppointmentRepository appointmentRepository,
            StockRepository stockRepository,
            DoctorRepository doctorRepository,
            MedicalDetailsRepository medicalDetailsRepository,
            PrescriptionMedsRepository prescriptionMedsRepository
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.currentAppointmentRepository = currentAppointmentRepository;
        this.appointmentRepository = appointmentRepository;
        this.stockRepository = stockRepository;
        this.doctorRepository = doctorRepository;
        this.medicalDetailsRepository = medicalDetailsRepository;
        this.prescriptionMedsRepository = prescriptionMedsRepository;
        
    }

    @Override
    public void submitPrescription(PrescriptionReq prescriptionDTO) {
        // get Doc Email
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String sapEmail = userDetails.getUsername();

        // Get Assigned Appointment
        CurrentAppointment byDoctorEmail = currentAppointmentRepository
                .findByDoctor_DoctorEmail(sapEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No Appointment Scheduled"));

        Prescription prescription = new Prescription();

        // Check Meds
        Map<UUID, Integer> instance = new HashMap<>();

        // Set Medicine
        for (PrescriptionMedsDTO pres : prescriptionDTO.getMeds()) {
            PrescriptionMeds medicine = new PrescriptionMeds();
            medicine.setDosageAfternoon(pres.getDosageAfternoon());
            medicine.setDosageEvening(pres.getDosageEvening());
            medicine.setDosageMorning(pres.getDosageMorning());
            medicine.setDuration(pres.getDuration());

            if (instance.get(pres.getMedicine()) != null)
                instance.put(pres.getMedicine(), instance.get(pres.getMedicine()) + 1);
            else
                instance.put(pres.getMedicine(), 1);

            if (pres.getDuration() < 1)
                throw new IllegalArgumentException("Duration Should be at least 1");

            Float medQty = pres.getDosageAfternoon() + pres.getDosageMorning() + pres.getDosageEvening();

            if (!(medQty > 0))
                throw new IllegalArgumentException("No medicine quantity defined");

            Stock currMed = stockRepository.findById(pres.getMedicine())
                    .orElseThrow(() -> new ResourceNotFoundException("No Such Medicine"));

            if (currMed.getQuantity() < (medQty * pres.getDuration()))
                throw new IllegalArgumentException("Not enough Stock available");

            if (currMed.getExpirationDate().isBefore(Instant.ofEpochMilli(System.currentTimeMillis())
                    .atZone(ZoneId.of("Asia/Kolkata")).toLocalDate()))
                throw new IllegalArgumentException("Medicines expired");

            medicine.setMedicine(currMed);
            medicine.setSuggestion(pres.getSuggestion());
            prescription.addPresMed(medicine);
        }

        // Check for duplicates
        for (UUID inst : instance.keySet()) {
            if (instance.get(inst) > 1)
                throw new IllegalArgumentException("Must Assign only 1 instance of each medicine");
        }

        prescription.setDiagnosis(prescriptionDTO.getDiagnosis());
        prescription.setDietaryRemarks(prescriptionDTO.getDietaryRemarks());
        prescription.setTestNeeded(prescriptionDTO.getTestNeeded());
        prescription.setPatient(byDoctorEmail.getPatient());
        prescription.setDoctor(byDoctorEmail.getDoctor());

        prescription = prescriptionRepository.save(prescription);

        // Set Prescription for appointment
        Appointment appointment = appointmentRepository
                .findByAppointmentId(byDoctorEmail.getAppointment().getAppointmentId());

        if (appointment.getPrescription() != null) {
            Prescription prescription2 = appointment.getPrescription();
            appointment.setPrescription(null);
            prescriptionRepository.delete(prescription2);
        }

        appointment.setPrescription(prescription);
        appointmentRepository.save(appointment);

        // Free Doctor
        Doctor doctor = doctorRepository.findByDoctorEmail(sapEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No Doctor Found"));
        doctor.setStatus(true);

        // Add to Completed Queue
        AppointmentQueueManager.addAppointedQueue(byDoctorEmail.getAppointment().getAppointmentId());

        // Save Doctor and Remove Doc from Current Appointment
        doctorRepository.save(doctor);
        byDoctorEmail.setDoctor(null);
        currentAppointmentRepository.save(byDoctorEmail);
    }

    @Override
    public ResponseEntity<?> getPrescription(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Appointment Scheduled"));

        PrescriptionRes prescriptionRes = new PrescriptionRes();

        appointment.getPatient().setPassword(""); // Hide password in response

        prescriptionRes.setPrescription(new PrescriptionDTO(appointment.getPrescription()));

        prescriptionRes.setDate(appointment.getDate());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        Date date = new Date(appointment.getTimestamp());

        prescriptionRes.setTime(simpleDateFormat.format(date));

        prescriptionRes.setResidenceType(
                medicalDetailsRepository.findByPatient_Email(appointment.getPatient().getEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("no Medical Details Found"))
                        .getResidenceType());

        return ResponseEntity.ok(prescriptionRes);
    }

    @Override
public Map<String, Integer> getDiagnosisFrequencies() {
    List<String> diagnosisList = prescriptionRepository.findAllDiagnosis();

    // Temporary map to group by normalized name (lowercase)
    Map<String, Integer> normalizedFrequencyMap = new HashMap<>();

    for (String diagnosis : diagnosisList) {
        if (diagnosis == null || diagnosis.trim().isEmpty()) {
            continue; // skip null or empty diagnoses
        }

        String normalized = diagnosis.trim().toLowerCase();  // Normalize to lowercase
        normalizedFrequencyMap.put(
            normalized,
            normalizedFrequencyMap.getOrDefault(normalized, 0) + 1
        );
    }

    // Create the final response map with capitalized names
    Map<String, Integer> capitalizedFrequencyMap = new HashMap<>();

    for (Map.Entry<String, Integer> entry : normalizedFrequencyMap.entrySet()) {
        String capitalized = capitalizeWords(entry.getKey());
        capitalizedFrequencyMap.put(capitalized, entry.getValue());
    }

    return capitalizedFrequencyMap;
}
private String capitalizeWords(String input) {
    if (input == null || input.isEmpty()) {
        return input;
    }

    String[] words = input.trim().toLowerCase().split("\\s+");
    StringBuilder result = new StringBuilder();

    for (String word : words) {
        if (!word.isEmpty()) {
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
        }
    }

    return result.toString().trim();
}

@Override
    public List<ActiveMedicationDTO> getActiveMedications(String sapEmail) {

        // Retrieve all prescribed medicines for the patient
        List<PrescriptionMeds> medsList = prescriptionMedsRepository.findPrescriptionMedsBySapEmail(sapEmail);

        // Retrieve all appointments for the patient
        List<Appointment> appointments = appointmentRepository.findByPatient_Email(sapEmail);

        // Map prescriptions to their respective appointments
        Map<UUID, Appointment> prescriptionAppointmentMap = new HashMap<>();
        for (Appointment appt : appointments) {
            if (appt.getPrescription() != null) {
                prescriptionAppointmentMap.put(appt.getPrescription().getPrescriptionId(), appt);
            }
        }

        LocalDate today = LocalDate.now();
        List<ActiveMedicationDTO> activeMeds = new ArrayList<>();

        // Iterate over medications and filter active ones
        for (PrescriptionMeds meds : medsList) {

            Prescription prescription = meds.getPrescription();
            if (prescription == null) {
                continue;
            }

            Appointment appointment = prescriptionAppointmentMap.get(prescription.getPrescriptionId());
            if (appointment == null) {
                continue;
            }

            LocalDate startDate = appointment.getDate();
            LocalDate endDate = startDate.plusDays(meds.getDuration());

            boolean isActive = (!today.isBefore(startDate)) && (!today.isAfter(endDate));

            if (isActive) {
                ActiveMedicationDTO dto = new ActiveMedicationDTO();
                dto.setMedicineName(meds.getMedicine().getMedicineName());

                Float totalDosage = meds.getDosageMorning() + meds.getDosageAfternoon() + meds.getDosageEvening();
                dto.setDosage(totalDosage.toString());

                dto.setDuration(meds.getDuration());
                dto.setAppointmentDate(startDate.toString());
                dto.setEndDate(endDate.toString());

                activeMeds.add(dto);
            }
        }

        return activeMeds;
    }

}
