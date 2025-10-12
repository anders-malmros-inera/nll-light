package se.inera.nll.nlllight.api.prescription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inera.nll.nlllight.api.adherence.AdherenceService;
import se.inera.nll.nlllight.api.adherence.dto.AdherenceRecordDTO;
import se.inera.nll.nlllight.api.adherence.dto.RecordAdherenceRequest;
import se.inera.nll.nlllight.api.prescription.dto.PrescriptionDTO;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prescriptions")
@Tag(name = "Prescriptions", description = "Patient prescription management API")
public class PrescriptionController {
    
    private final PrescriptionService prescriptionService;
    private final AdherenceService adherenceService;
    
    public PrescriptionController(PrescriptionService prescriptionService,
                                 AdherenceService adherenceService) {
        this.prescriptionService = prescriptionService;
        this.adherenceService = adherenceService;
    }
    
    @GetMapping
    @Operation(summary = "Get my prescriptions", 
               description = "Returns all prescriptions for the authenticated patient")
    public ResponseEntity<List<PrescriptionDTO>> getMyPrescriptions(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-Patient-Id", required = false) String patientId) {
        
        // TODO: Extract patientId from authentication token instead of header
        if (patientId == null) {
            patientId = "patient-001"; // Default for testing
        }
        
        // Default to ACTIVE prescriptions if status not specified
        if (status == null) {
            status = "ACTIVE";
        }
        
        List<PrescriptionDTO> prescriptions = prescriptionService.getPatientPrescriptions(patientId, status);
        return ResponseEntity.ok(prescriptions);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get prescription details", 
               description = "Returns detailed information about a specific prescription")
    public ResponseEntity<PrescriptionDTO> getPrescription(@PathVariable Long id) {
        PrescriptionDTO prescription = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(prescription);
    }
    
    @GetMapping("/refill-eligible")
    @Operation(summary = "Get refill-eligible prescriptions",
               description = "Returns prescriptions that are eligible for refill")
    public ResponseEntity<List<PrescriptionDTO>> getRefillEligiblePrescriptions(
            @RequestHeader(value = "X-Patient-Id", required = false) String patientId) {
        
        if (patientId == null) {
            patientId = "patient-001"; // Default for testing
        }
        
        List<PrescriptionDTO> prescriptions = prescriptionService.getRefillEligiblePrescriptions(patientId);
        return ResponseEntity.ok(prescriptions);
    }
    
    @PostMapping("/{id}/take")
    @Operation(summary = "Record taking medication",
               description = "Records that a patient has taken (or missed) their medication")
    public ResponseEntity<AdherenceRecordDTO> recordMedicationTaken(
            @PathVariable Long id,
            @RequestBody RecordAdherenceRequest request,
            @RequestHeader(value = "X-Patient-Id", required = false) String patientId) {
        
        if (patientId == null) {
            patientId = "patient-001"; // Default for testing
        }
        
        AdherenceRecordDTO record = adherenceService.recordAdherence(
                id, 
                patientId,
                request.getStatus(),
                request.getNotes()
        );
        
        return ResponseEntity.ok(record);
    }
    
    @GetMapping("/{id}/adherence")
    @Operation(summary = "Get adherence history",
               description = "Returns adherence history for a prescription")
    public ResponseEntity<List<AdherenceRecordDTO>> getAdherenceHistory(@PathVariable Long id) {
        List<AdherenceRecordDTO> records = adherenceService.getAdherenceHistory(id);
        return ResponseEntity.ok(records);
    }
}
