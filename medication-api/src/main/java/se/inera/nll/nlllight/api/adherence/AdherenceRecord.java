package se.inera.nll.nlllight.api.adherence;

import jakarta.persistence.*;
import se.inera.nll.nlllight.api.common.AdherenceStatus;
import se.inera.nll.nlllight.api.common.RecordSource;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.prescription.Prescription;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "adherence_records")
public class AdherenceRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;
    
    @Column(name = "actual_time")
    private LocalDateTime actualTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdherenceStatus status;
    
    @Column(name = "dose_taken", precision = 10, scale = 2)
    private BigDecimal doseTaken;
    
    @Column(name = "dose_unit", length = 20)
    private String doseUnit;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "side_effects_reported", columnDefinition = "TEXT")
    private String sideEffectsReported;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RecordSource source = RecordSource.PATIENT_REPORTED;
    
    @Column(name = "device_id", length = 100)
    private String deviceId;
    
    @Column(name = "location_type", length = 50)
    private String locationType;
    
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;
    
    @Column(name = "reminder_acknowledged")
    private Boolean reminderAcknowledged = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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
    
    public Prescription getPrescription() {
        return prescription;
    }
    
    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }
    
    public Patient getPatient() {
        return patient;
    }
    
    public void setPatient(Patient patient) {
        this.patient = patient;
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
    
    public String getSideEffectsReported() {
        return sideEffectsReported;
    }
    
    public void setSideEffectsReported(String sideEffectsReported) {
        this.sideEffectsReported = sideEffectsReported;
    }
    
    public RecordSource getSource() {
        return source;
    }
    
    public void setSource(RecordSource source) {
        this.source = source;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getLocationType() {
        return locationType;
    }
    
    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }
    
    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }
    
    public void setReminderSentAt(LocalDateTime reminderSentAt) {
        this.reminderSentAt = reminderSentAt;
    }
    
    public Boolean getReminderAcknowledged() {
        return reminderAcknowledged;
    }
    
    public void setReminderAcknowledged(Boolean reminderAcknowledged) {
        this.reminderAcknowledged = reminderAcknowledged;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
