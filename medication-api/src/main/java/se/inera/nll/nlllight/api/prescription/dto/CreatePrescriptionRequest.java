package se.inera.nll.nlllight.api.prescription.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreatePrescriptionRequest {
    
    @NotNull(message = "Patient ID is required")
    private String patientId;
    
    @NotNull(message = "Medication ID is required")
    private Long medicationId;
    
    @NotNull(message = "Dose is required")
    @Positive(message = "Dose must be positive")
    private BigDecimal dose;
    
    @NotNull(message = "Dose unit is required")
    private String doseUnit;
    
    @NotNull(message = "Frequency is required")
    private String frequency;
    
    private String frequencyDescription;
    
    @NotNull(message = "Route is required")
    private String route;
    
    private String indication;
    private String instructions;
    
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantityPrescribed;
    
    private String quantityUnit;
    
    private Integer daysSupply;
    private Integer refillsAllowed;
    
    private Boolean isPRN = false;
    private Boolean isSubstitutionAllowed = true;
    
    // Getters and Setters
    
    public String getPatientId() {
        return patientId;
    }
    
    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
    
    public Long getMedicationId() {
        return medicationId;
    }
    
    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }
    
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
    
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public Integer getQuantityPrescribed() {
        return quantityPrescribed;
    }
    
    public void setQuantityPrescribed(Integer quantityPrescribed) {
        this.quantityPrescribed = quantityPrescribed;
    }
    
    public String getQuantityUnit() {
        return quantityUnit;
    }
    
    public void setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
    }
    
    public Integer getDaysSupply() {
        return daysSupply;
    }
    
    public void setDaysSupply(Integer daysSupply) {
        this.daysSupply = daysSupply;
    }
    
    public Integer getRefillsAllowed() {
        return refillsAllowed;
    }
    
    public void setRefillsAllowed(Integer refillsAllowed) {
        this.refillsAllowed = refillsAllowed;
    }
    
    public Boolean getIsPRN() {
        return isPRN;
    }
    
    public void setIsPRN(Boolean isPRN) {
        this.isPRN = isPRN;
    }
    
    public Boolean getIsSubstitutionAllowed() {
        return isSubstitutionAllowed;
    }
    
    public void setIsSubstitutionAllowed(Boolean isSubstitutionAllowed) {
        this.isSubstitutionAllowed = isSubstitutionAllowed;
    }
}
