package se.inera.nll.nlllight.api.pharmacist;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import se.inera.nll.nlllight.api.prescription.PrescriptionService;
import se.inera.nll.nlllight.api.prescription.dto.DispenseMedicationRequest;
import se.inera.nll.nlllight.api.prescription.dto.PrescriptionDTO;

@RestController
@RequestMapping("/api/v1/pharmacist")
@Tag(name = "Pharmacist", description = "Pharmacist medication dispensing API")
public class PharmacistController {
    
    private final PrescriptionService prescriptionService;
    
    public PharmacistController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }
    
    @PostMapping("/prescriptions/dispense")
    @PreAuthorize("hasAnyAuthority('ROLE_PHARMACIST', 'PHARMACIST')")
    @Operation(summary = "Dispense medication", 
               description = "Dispenses medication for a prescription, deducting from the prescribed quantity")
    public ResponseEntity<PrescriptionDTO> dispenseMedication(
            @Valid @RequestBody DispenseMedicationRequest request,
            @RequestHeader(value = "X-Pharmacist-Id", required = false) String pharmacistId) {
        
        // TODO: Extract pharmacistId from authentication token
        if (pharmacistId == null) {
            pharmacistId = "pharmacist1"; // Default for testing
        }
        
        PrescriptionDTO prescription = prescriptionService.dispenseMedication(request, pharmacistId);
        return ResponseEntity.ok(prescription);
    }
}