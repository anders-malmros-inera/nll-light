package se.inera.nll.nlllight.api.adherence.dto;

import se.inera.nll.nlllight.api.common.AdherenceStatus;

public class RecordAdherenceRequest {
    
    private AdherenceStatus status;
    private String notes;
    
    public AdherenceStatus getStatus() {
        return status;
    }
    
    public void setStatus(AdherenceStatus status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
