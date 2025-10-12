package se.inera.nll.nlllight.api.adherence.dto;

import se.inera.nll.nlllight.api.common.AdherenceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdherenceRecordDTO {
    
    private Long id;
    private Long prescriptionId;
    private String medicationName;
    private LocalDateTime scheduledTime;
    private LocalDateTime actualTime;
    private AdherenceStatus status;
    private BigDecimal doseTaken;
    private String doseUnit;
    private String notes;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPrescriptionId() {
        return prescriptionId;
    }
    
    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }
    
    public String getMedicationName() {
        return medicationName;
    }
    
    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }
    
    public LocalDateTime getScheduledTime() {
        return scheduledTime;
    }
    
    public void setScheduledTime(LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
    
    public LocalDateTime getActualTime() {
        return actualTime;
    }
    
    public void setActualTime(LocalDateTime actualTime) {
        this.actualTime = actualTime;
    }
    
    public AdherenceStatus getStatus() {
        return status;
    }
    
    public void setStatus(AdherenceStatus status) {
        this.status = status;
    }
    
    public BigDecimal getDoseTaken() {
        return doseTaken;
    }
    
    public void setDoseTaken(BigDecimal doseTaken) {
        this.doseTaken = doseTaken;
    }
    
    public String getDoseUnit() {
        return doseUnit;
    }
    
    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
