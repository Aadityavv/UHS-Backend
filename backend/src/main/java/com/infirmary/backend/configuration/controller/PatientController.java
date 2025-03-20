package com.infirmary.backend.configuration.controller;

import com.infirmary.backend.configuration.Exception.AppointmentNotFoundException;
import com.infirmary.backend.configuration.Exception.DoctorNotFoundException;
import com.infirmary.backend.configuration.Exception.MedicalDetailsNotFoundException;
import com.infirmary.backend.configuration.Exception.PatientNotFoundException;
import com.infirmary.backend.configuration.dto.AppointmentReqDTO;
import com.infirmary.backend.configuration.dto.DoctorDTO;
import com.infirmary.backend.configuration.dto.MedicalDetailsDTO;
import com.infirmary.backend.configuration.dto.PatientDTO;
import com.infirmary.backend.configuration.dto.PatientDetailsResponseDTO;
import com.infirmary.backend.configuration.service.AppointmentService;
import com.infirmary.backend.configuration.service.DoctorService;
import com.infirmary.backend.configuration.service.PatientService;
import com.infirmary.backend.configuration.service.PrescriptionService;
import com.infirmary.backend.configuration.dto.ActiveMedicationDTO;


import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.infirmary.backend.shared.utility.FunctionUtil.createSuccessResponse;

import java.lang.String;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping(value = "/api/patient")
@Validated
public class PatientController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;


    public PatientController(PatientService patientService, DoctorService doctorService, AppointmentService appointmentService, PrescriptionService prescriptionService) {
            this.patientService = patientService;
            this.doctorService = doctorService;
            this.appointmentService = appointmentService;
            this.prescriptionService = prescriptionService;

    }

    private static String getTokenClaims(){
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUsername();
    }

    //Get Patient details
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping(value = "/")
    public ResponseEntity<?> getPatientBySapEmail()
            throws PatientNotFoundException {
        PatientDTO response = patientService.getPatientBySapEmail(getTokenClaims());
        return createSuccessResponse(response);
    }

    //Update medical details of the patient
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @PutMapping(value = "/update")
    public ResponseEntity<?> updatePatient(@Valid @RequestBody MedicalDetailsDTO medicalDetailsDTO,BindingResult bindingResult) throws
            PatientNotFoundException, MedicalDetailsNotFoundException {
        MedicalDetailsDTO response = patientService.updatePatientDetails(getTokenClaims(), medicalDetailsDTO);
        return createSuccessResponse(response);
    }

    //Get All relevant details of the patient
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping(value = "/getAllDetails")
    public ResponseEntity<?> getAllDetails() throws
            PatientNotFoundException, MedicalDetailsNotFoundException {
        PatientDetailsResponseDTO response = patientService.getAllDetails(getTokenClaims());
        return createSuccessResponse(response);
    }

    //Submit the appointment form
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @PostMapping(value = "/submitAppointment")
    public ResponseEntity<?> submitAppointmnent(@Valid @RequestBody AppointmentReqDTO appointmentReqDTO, BindingResult bindingResult,@RequestHeader(name = "X-Latitude",required = true) Double latitude,@RequestHeader(name = "X-Longitude", required = true) Double longitude) {
        return patientService.submitAppointment(getTokenClaims(),appointmentReqDTO,latitude,longitude);
    }

    //Get Status for the the patient like appointment status and doctor status
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping(value = "/getStatus")
    public ResponseEntity<?> getAppointmentStatus(){
        return patientService.getStatus(getTokenClaims());
    }

    //Get Patient Token Number
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping(value = "/getToken")
    public ResponseEntity<?> getTokenNo(){
        return patientService.getToken(getTokenClaims());
    }

    //Get Prescription for patient
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping("/getPrescription/{Id}")
    public ResponseEntity<?> getPrescription(@PathVariable UUID Id){
        return patientService.getPrescriptions(getTokenClaims(),Id);
    }

    //Get All the prescriptions for the patient
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping("/getAppointment")
    public ResponseEntity<?> getAppointment(){
        return patientService.getAppointment(getTokenClaims());
    }

    //Get All Available doctor
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping(value = "/getAvailableDoctors")
    public ResponseEntity<?> getAllAvailableDoctors(@RequestHeader(name = "X-Latitude",required = true) Double latitude,@RequestHeader(name = "X-Longitude", required = true) Double longitude) throws DoctorNotFoundException {
        List<DoctorDTO> list = doctorService.getAvailableDoctors(latitude,longitude);
        return createSuccessResponse(list);
    }

    //Get Last Appointment Date for Patient
    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping(value = "/lastAppointmentDate")
    public ResponseEntity<?> getLastAppointmentDate() throws AppointmentNotFoundException {
        LocalDate response = appointmentService.getLastAppointmentDateByEmail(getTokenClaims());
        return createSuccessResponse(response);
    }

    // Admin-only endpoint to get total patient count
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getPatientCount() {
        long count = patientService.getTotalPatientCount();
        return ResponseEntity.ok(Collections.singletonMap("count", count));
    }

    @PreAuthorize("hasRole('ROLE_PATIENT')")
    @GetMapping("/medications/active/{sapEmail}")
    public ResponseEntity<List<ActiveMedicationDTO>> getActiveMedications(@PathVariable String sapEmail) {
    List<ActiveMedicationDTO> activeMeds = prescriptionService.getActiveMedications(sapEmail);
    return ResponseEntity.ok(activeMeds);
}

    
}
