package com.infirmary.backend.configuration.dto;

import java.util.UUID;

import com.infirmary.backend.configuration.model.Appointment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteAptSave {
    private String patEmail;
    private String docEmail;
    private String reason;
    private Long locId;
    private UUID prevAptId;
    private Long timestamp;
    
    public DeleteAptSave(Appointment appointment) {
        this.patEmail = appointment.getPatient().getEmail();
        this.docEmail = appointment.getDoctor() != null ? appointment.getDoctor().getDoctorEmail() : null;
        this.reason = appointment.getAptForm().getReason();
        this.locId = appointment.getLocation().getLocId();
    
        var aptForm = appointment.getAptForm();
        var prevApt = aptForm.getPrevAppointment();
    
        this.prevAptId = (Boolean.TRUE.equals(aptForm.getIsFollowUp()) && prevApt != null)
            ? prevApt.getAppointmentId()
            : null;
    
        this.timestamp = appointment.getTimestamp();
    }
    

}
