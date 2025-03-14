package com.infirmary.backend.configuration.service;

import com.infirmary.backend.configuration.Exception.MedicalDetailsNotFoundException;
import com.infirmary.backend.configuration.Exception.PatientNotFoundException;
import com.infirmary.backend.configuration.dto.AppointmentReqDTO;
import com.infirmary.backend.configuration.dto.MedicalDetailsDTO;
import com.infirmary.backend.configuration.dto.PatientDTO;
import com.infirmary.backend.configuration.dto.PatientDetailsResponseDTO;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface PatientService {
    
    /**
     * Retrieves patient details by SAP email
     * @param sapEmail The SAP email of the patient
     * @return PatientDTO containing patient details
     * @throws PatientNotFoundException If no patient is found with the given email
     */
    PatientDTO getPatientBySapEmail(String sapEmail) throws PatientNotFoundException;

    /**
     * Validates patient data during registration/update
     * @param patientDTO Patient data transfer object
     */
    void validatePatientData(PatientDTO patientDTO);

    /**
     * Updates medical details for a patient
     * @param sapEmail SAP email of the patient
     * @param medicalDetailsDTO Updated medical details
     * @return Updated MedicalDetailsDTO
     * @throws PatientNotFoundException If patient not found
     * @throws MedicalDetailsNotFoundException If medical details record not found
     */
    MedicalDetailsDTO updatePatientDetails(String sapEmail, MedicalDetailsDTO medicalDetailsDTO)
            throws PatientNotFoundException, MedicalDetailsNotFoundException;

    /**
     * Retrieves complete patient details including medical history
     * @param sapEmail SAP email of the patient
     * @return Combined patient and medical details
     * @throws PatientNotFoundException If patient not found
     * @throws MedicalDetailsNotFoundException If medical details not found
     */
    PatientDetailsResponseDTO getAllDetails(String sapEmail) 
            throws PatientNotFoundException, MedicalDetailsNotFoundException;

    /**
     * Submits a new appointment request
     * @param sapEmail SAP email of the patient
     * @param appointmentReqDTO Appointment request details
     * @param latitude Geographic latitude
     * @param longitude Geographic longitude
     * @return ResponseEntity with appointment confirmation
     */
    ResponseEntity<?> submitAppointment(String sapEmail, AppointmentReqDTO appointmentReqDTO, 
                                      Double latitude, Double longitude);

    /**
     * Retrieves current status of patient's appointments
     * @param sapEmail SAP email of the patient
     * @return ResponseEntity with status information
     */
    ResponseEntity<?> getStatus(String sapEmail);

    /**
     * Retrieves patient's current token number
     * @param sapEmail SAP email of the patient
     * @return ResponseEntity with token information
     * @throws ResourceNotFoundException If no token is allocated
     */
    ResponseEntity<?> getToken(String sapEmail) throws ResourceNotFoundException;

    /**
     * Retrieves prescriptions for a specific appointment
     * @param sapEmail SAP email of the patient
     * @param aptId UUID of the appointment
     * @return ResponseEntity with prescription details
     */
    ResponseEntity<?> getPrescriptions(String sapEmail, UUID aptId);

    /**
     * Retrieves all appointments for a patient
     * @param sapEmail SAP email of the patient
     * @return ResponseEntity with list of appointments
     */
    ResponseEntity<?> getAppointment(String sapEmail);

    /**
     * Retrieves total count of registered patients (Admin-only)
     * @return Total number of patients in the system
     */
    long getTotalPatientCount();
}