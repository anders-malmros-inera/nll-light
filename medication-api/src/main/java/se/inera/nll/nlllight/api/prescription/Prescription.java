package se.inera.nll.nlllight.api.prescription;

import jakarta.persistence.*;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.prescriber.Prescriber;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
public class Prescription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medication_id", nullable = false)
    private Medication medication;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescriber_id")
    private Prescriber prescriber;
    
    @Column(name = "prescription_number", unique = true, nullable = false, length = 50)
    private String prescriptionNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionStatus status = PrescriptionStatus.ACTIVE;
    
    // Dosing information
    @Column(precision = 10, scale = 2)
    private BigDecimal dose;
    
    @Column(name = "dose_unit", length = 20)
    private String doseUnit;
    
    @Column(length = 20)
    private String frequency;
    
    @Column(name = "frequency_description", columnDefinition = "TEXT")
    private String frequencyDescription;
    
    @Column(length = 50)
    private String route;
    
    @Column(name = "max_daily_dose", precision = 10, scale = 2)
    private BigDecimal maxDailyDose;
    
    @Column(name = "max_daily_dose_unit", length = 20)
    private String maxDailyDoseUnit;
    
    // Clinical information
    @Column(columnDefinition = "TEXT")
    private String indication;
    
    @Column(columnDefinition = "TEXT")
    private String instructions;
    
    @Column(name = "clinical_notes", columnDefinition = "TEXT")
    private String clinicalNotes;
    
    // Temporal
    @Column(name = "prescribed_date", nullable = false)
    private LocalDate prescribedDate;
    
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    // Refills
    @Column(name = "refills_allowed")
    private Integer refillsAllowed = 0;
    
    @Column(name = "refills_remaining")
    private Integer refillsRemaining = 0;
    
    @Column(name = "last_refill_date")
    private LocalDate lastRefillDate;
    
    @Column(name = "next_refill_eligible_date")
    private LocalDate nextRefillEligibleDate;
    
    // Quantity
    @Column(name = "quantity_prescribed")
    private Integer quantityPrescribed;
    
    @Column(name = "quantity_dispensed", nullable = false)
    private Integer quantityDispensed = 0;
    
    @Column(name = "quantity_unit", length = 20)
    private String quantityUnit;
    
    @Column(name = "days_supply")
    private Integer daysSupply;
    
    // Flags
    @Column(name = "is_prn")
    private Boolean isPRN = false;
    
    @Column(name = "is_substitution_allowed")
    private Boolean isSubstitutionAllowed = true;
    
    @Column(name = "requires_prior_auth")
    private Boolean requiresPriorAuthorization = false;
    
    @Column(name = "prior_auth_number", length = 50)
    private String priorAuthNumber;
    
    @Column(name = "is_controlled_substance")
    private Boolean isControlledSubstance = false;
    
    // External references
    @Column(name = "external_prescription_id", length = 100)
    private String externalPrescriptionId;
    
    @Column(name = "external_system", length = 50)
    private String externalSystem;
    
    // Audit
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    // Cancellation
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (quantityDispensed == null) {
            quantityDispensed = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Patient getPatient() {
        return patient;
    }
    
    public void setPatient(Patient patient) {
        this.patient = patient;
    }
    
    public Medication getMedication() {
        return medication;
    }
    
    public void setMedication(Medication medication) {
        this.medication = medication;
    }
    
    public Prescriber getPrescriber() {
        return prescriber;
    }
    
    public void setPrescriber(Prescriber prescriber) {
        this.prescriber = prescriber;
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
    
    public BigDecimal getMaxDailyDose() {
        return maxDailyDose;
    }
    
    public void setMaxDailyDose(BigDecimal maxDailyDose) {
        this.maxDailyDose = maxDailyDose;
    }
    
    public String getMaxDailyDoseUnit() {
        return maxDailyDoseUnit;
    }
    
    public void setMaxDailyDoseUnit(String maxDailyDoseUnit) {
        this.maxDailyDoseUnit = maxDailyDoseUnit;
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
    
    public String getClinicalNotes() {
        return clinicalNotes;
    }
    
    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
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
    
    public LocalDate getLastRefillDate() {
        return lastRefillDate;
    }
    
    public void setLastRefillDate(LocalDate lastRefillDate) {
        this.lastRefillDate = lastRefillDate;
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
    
    public Boolean getRequiresPriorAuthorization() {
        return requiresPriorAuthorization;
    }
    
    public void setRequiresPriorAuthorization(Boolean requiresPriorAuthorization) {
        this.requiresPriorAuthorization = requiresPriorAuthorization;
    }
    
    public String getPriorAuthNumber() {
        return priorAuthNumber;
    }
    
    public void setPriorAuthNumber(String priorAuthNumber) {
        this.priorAuthNumber = priorAuthNumber;
    }
    
    public Boolean getIsControlledSubstance() {
        return isControlledSubstance;
    }
    
    public void setIsControlledSubstance(Boolean isControlledSubstance) {
        this.isControlledSubstance = isControlledSubstance;
    }
    
    public String getExternalPrescriptionId() {
        return externalPrescriptionId;
    }
    
    public void setExternalPrescriptionId(String externalPrescriptionId) {
        this.externalPrescriptionId = externalPrescriptionId;
    }
    
    public String getExternalSystem() {
        return externalSystem;
    }
    
    public void setExternalSystem(String externalSystem) {
        this.externalSystem = externalSystem;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
    
    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
    
    public String getCancelledBy() {
        return cancelledBy;
    }
    
    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
    
    public String getCancellationReason() {
        return cancellationReason;
    }
    
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
