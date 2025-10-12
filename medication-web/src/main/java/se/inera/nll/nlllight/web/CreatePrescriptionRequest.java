package se.inera.nll.nlllight.web;

public class CreatePrescriptionRequest {
    private String patientId;
    private Long medicationId;
    private String strength;
    private String form;
    private String instructions;
    private String startDate;
    private String endDate;
    private Integer refillsAllowed;
    private String prescriberId;
    
    // Getters and Setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    
    public Long getMedicationId() { return medicationId; }
    public void setMedicationId(Long medicationId) { this.medicationId = medicationId; }
    
    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }
    
    public String getForm() { return form; }
    public void setForm(String form) { this.form = form; }
    
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    
    public Integer getRefillsAllowed() { return refillsAllowed; }
    public void setRefillsAllowed(Integer refillsAllowed) { this.refillsAllowed = refillsAllowed; }
    
    public String getPrescriberId() { return prescriberId; }
    public void setPrescriberId(String prescriberId) { this.prescriberId = prescriberId; }
}
