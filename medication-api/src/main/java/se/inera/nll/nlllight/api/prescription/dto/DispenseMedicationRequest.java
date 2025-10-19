package se.inera.nll.nlllight.api.prescription.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DispenseMedicationRequest {
    
    @NotNull(message = "Prescription ID is required")
    private Long prescriptionId;
    
    @NotNull(message = "Quantity to dispense is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantityToDispense;
    
    private String notes;
    
    // Getters and Setters
    
    public Long getPrescriptionId() {
        return prescriptionId;
    }
    
    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }
    
    public Integer getQuantityToDispense() {
        return quantityToDispense;
    }
    
    public void setQuantityToDispense(Integer quantityToDispense) {
        this.quantityToDispense = quantityToDispense;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}