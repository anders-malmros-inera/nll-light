package se.inera.nll.nlllight.api.prescription.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdatePrescriptionRequest {
    
    private BigDecimal dose;
    private String doseUnit;
    private String frequency;
    private String frequencyDescription;
    private String route;
    private String indication;
    private String instructions;
    private LocalDate endDate;
    private Integer refillsAllowed;
    private Boolean isSubstitutionAllowed;
    
    @NotNull(message = "Reason for modification is required")
    private String modificationReason;
    
    // Getters and Setters
    
    public BigDecimal getDose() {
        return dose;
    }
    
    public void setDose(BigDecimal dose) {
        this.dose = dose;
    }
    
    public String getDoseUnit() {
        return doseUnit;
    }
    
    public void setDoseUnit(String doseUnit) {
        this.doseUnit = doseUnit;
    }
    
    public String getFrequency() {
        return frequency;
    }
    
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }
    
    public String getFrequencyDescription() {
        return frequencyDescription;
    }
    
    public void setFrequencyDescription(String frequencyDescription) {
        this.frequencyDescription = frequencyDescription;
    }
    
    public String getRoute() {
        return route;
    }
    
    public void setRoute(String route) {
        this.route = route;
    }
    
    public String getIndication() {
        return indication;
    }
    
    public void setIndication(String indication) {
        this.indication = indication;
    }
    
    public String getInstructions() {
        return instructions;
    }
    
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Integer getRefillsAllowed() {
        return refillsAllowed;
    }
    
    public void setRefillsAllowed(Integer refillsAllowed) {
        this.refillsAllowed = refillsAllowed;
    }
    
    public Boolean getIsSubstitutionAllowed() {
        return isSubstitutionAllowed;
    }
    
    public void setIsSubstitutionAllowed(Boolean isSubstitutionAllowed) {
        this.isSubstitutionAllowed = isSubstitutionAllowed;
    }
    
    public String getModificationReason() {
        return modificationReason;
    }
    
    public void setModificationReason(String modificationReason) {
        this.modificationReason = modificationReason;
    }
}
