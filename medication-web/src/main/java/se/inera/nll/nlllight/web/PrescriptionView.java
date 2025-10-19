package se.inera.nll.nlllight.web;

import java.time.LocalDate;

public class PrescriptionView {
    private Long id;
    private String medicationName;
    private String medicationStrength;
    private String medicationForm;
    private String instructions;
    private LocalDate prescribedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer refillsRemaining;
    private String prescriberName;
    private String prescriberSpecialty;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    
    public String getMedicationStrength() { return medicationStrength; }
    public void setMedicationStrength(String medicationStrength) { this.medicationStrength = medicationStrength; }
    
    public String getMedicationForm() { return medicationForm; }
    public void setMedicationForm(String medicationForm) { this.medicationForm = medicationForm; }
    
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    
    public LocalDate getPrescribedDate() { return prescribedDate; }
    public void setPrescribedDate(LocalDate prescribedDate) { this.prescribedDate = prescribedDate; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getRefillsRemaining() { return refillsRemaining; }
    public void setRefillsRemaining(Integer refillsRemaining) { this.refillsRemaining = refillsRemaining; }
    
    public String getPrescriberName() { return prescriberName; }
    public void setPrescriberName(String prescriberName) { this.prescriberName = prescriberName; }
    
    public String getPrescriberSpecialty() { return prescriberSpecialty; }
    public void setPrescriberSpecialty(String prescriberSpecialty) { this.prescriberSpecialty = prescriberSpecialty; }
}
