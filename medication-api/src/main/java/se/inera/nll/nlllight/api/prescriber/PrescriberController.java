package se.inera.nll.nlllight.api.prescriber;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import se.inera.nll.nlllight.api.prescription.PrescriptionService;
import se.inera.nll.nlllight.api.prescription.dto.CreatePrescriptionRequest;
import se.inera.nll.nlllight.api.prescription.dto.PrescriptionDTO;
import se.inera.nll.nlllight.api.prescription.dto.UpdatePrescriptionRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/prescriber")
@Tag(name = "Prescriber", description = "Prescriber prescription management API")
public class PrescriberController {
    
    private final PrescriptionService prescriptionService;
    
    public PrescriberController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }
    
    @PostMapping("/prescriptions")
    @PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")
    @Operation(summary = "Create prescription", 
               description = "Creates a new prescription for a patient")
    public ResponseEntity<PrescriptionDTO> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest request,
            @RequestHeader(value = "X-Prescriber-Id", required = false) String prescriberId) {
        
        // TODO: Extract prescriberId from authentication token
        if (prescriberId == null) {
            prescriberId = "prescriber1"; // Default for testing
        }
        
        PrescriptionDTO prescription = prescriptionService.createPrescription(request, prescriberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(prescription);
    }
    
    @PutMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")
    @Operation(summary = "Update prescription", 
               description = "Updates an existing prescription")
    public ResponseEntity<PrescriptionDTO> updatePrescription(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdatePrescriptionRequest request,
            @RequestHeader(value = "X-Prescriber-Id", required = false) String prescriberId) {
        
        // TODO: Extract prescriberId from authentication token
        if (prescriberId == null) {
            prescriberId = "prescriber-001"; // Default for testing
        }
        
        PrescriptionDTO prescription = prescriptionService.updatePrescription(id, request, prescriberId);
        return ResponseEntity.ok(prescription);
    }
    
    @DeleteMapping("/prescriptions/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")
    @Operation(summary = "Cancel prescription", 
               description = "Cancels an existing prescription")
    public ResponseEntity<Void> cancelPrescription(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-Prescriber-Id", required = false) String prescriberId) {
        
        // TODO: Extract prescriberId from authentication token
        if (prescriberId == null) {
            prescriberId = "prescriber-001"; // Default for testing
        }
        
        String reason = request.getOrDefault("reason", "No reason provided");
        prescriptionService.cancelPrescription(id, reason, prescriberId);
        
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/prescriptions")
    @Operation(summary = "Get my prescriptions", 
               description = "Returns all prescriptions created by this prescriber")
    public ResponseEntity<List<PrescriptionDTO>> getMyPrescriptions(
            @RequestParam(required = false) String patientId,
            @RequestHeader(value = "X-Prescriber-Id", required = false) String prescriberId) {
        
        // TODO: Extract prescriberId from authentication token
        if (prescriberId == null) {
            prescriberId = "prescriber-001"; // Default for testing
        }
        
        List<PrescriptionDTO> prescriptions = prescriptionService.getPrescriberPrescriptions(prescriberId, patientId);
        return ResponseEntity.ok(prescriptions);
    }
    
    @GetMapping("/prescriptions/{id}")
    @Operation(summary = "Get prescription details", 
               description = "Returns detailed information about a specific prescription")
    public ResponseEntity<PrescriptionDTO> getPrescription(@PathVariable("id") Long id) {
        PrescriptionDTO prescription = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(prescription);
    }
}
