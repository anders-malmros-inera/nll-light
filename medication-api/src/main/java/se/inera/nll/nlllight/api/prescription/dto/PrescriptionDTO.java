package se.inera.nll.nlllight.api.prescription.dto;

import se.inera.nll.nlllight.api.common.PrescriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PrescriptionDTO {
    
    private Long id;
    private String prescriptionNumber;
    private PrescriptionStatus status;
    
    // Medication info
    private Long medicationId;
    private String medicationName;
    private String medicationStrength;
    private String medicationForm;
    
    // Prescriber info
    private String prescriberName;
    private String prescriberSpecialty;
    
    // Dosing
    private BigDecimal dose;
    private String doseUnit;
    private String frequency;
    private String frequencyDescription;
    private String route;
    
    // Clinical
    private String indication;
    private String instructions;
    
    // Temporal
    private LocalDate prescribedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    
    // Refills
    private Integer refillsAllowed;
    private Integer refillsRemaining;
    private LocalDate nextRefillEligibleDate;
    
    // Quantity
    private Integer quantityPrescribed;
    private Integer quantityDispensed;
    private String quantityUnit;
    private Integer daysSupply;
    
    // Flags
    private Boolean isPRN;
    private Boolean isSubstitutionAllowed;
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPrescriptionNumber() {
        return prescriptionNumber;
    }
    
    public void setPrescriptionNumber(String prescriptionNumber) {
        this.prescriptionNumber = prescriptionNumber;
    }
    
    public PrescriptionStatus getStatus() {
        return status;
    }
    
    public void setStatus(PrescriptionStatus status) {
        this.status = status;
    }
    
    public Long getMedicationId() {
        return medicationId;
    }
    
    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }
    
    public String getMedicationName() {
        return medicationName;
    }
    
    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }
    
    public String getMedicationStrength() {
        return medicationStrength;
    }
    
    public void setMedicationStrength(String medicationStrength) {
        this.medicationStrength = medicationStrength;
    }
    
    public String getMedicationForm() {
        return medicationForm;
    }
    
    public void setMedicationForm(String medicationForm) {
        this.medicationForm = medicationForm;
    }
    
    public String getPrescriberName() {
        return prescriberName;
    }
    
    public void setPrescriberName(String prescriberName) {
        this.prescriberName = prescriberName;
    }
    
    public String getPrescriberSpecialty() {
        return prescriberSpecialty;
    }
    
    public void setPrescriberSpecialty(String prescriberSpecialty) {
        this.prescriberSpecialty = prescriberSpecialty;
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
    
    public LocalDate getPrescribedDate() {
        return prescribedDate;
    }
    
    public void setPrescribedDate(LocalDate prescribedDate) {
        this.prescribedDate = prescribedDate;
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
    
    public Integer getRefillsAllowed() {
        return refillsAllowed;
    }
    
    public void setRefillsAllowed(Integer refillsAllowed) {
        this.refillsAllowed = refillsAllowed;
    }
    
    public Integer getRefillsRemaining() {
        return refillsRemaining;
    }
    
    public void setRefillsRemaining(Integer refillsRemaining) {
        this.refillsRemaining = refillsRemaining;
    }
    
    public LocalDate getNextRefillEligibleDate() {
        return nextRefillEligibleDate;
    }
    
    public void setNextRefillEligibleDate(LocalDate nextRefillEligibleDate) {
        this.nextRefillEligibleDate = nextRefillEligibleDate;
    }
    
    public Integer getQuantityPrescribed() {
        return quantityPrescribed;
    }
    
    public void setQuantityPrescribed(Integer quantityPrescribed) {
        this.quantityPrescribed = quantityPrescribed;
    }
    
    public Integer getQuantityDispensed() {
        return quantityDispensed;
    }
    
    public void setQuantityDispensed(Integer quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
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
