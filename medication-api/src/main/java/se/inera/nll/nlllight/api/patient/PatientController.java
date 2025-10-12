package se.inera.nll.nlllight.api.patient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.medication.MedicationRepository;
import se.inera.nll.nlllight.api.prescription.Prescription;
import se.inera.nll.nlllight.api.prescription.PrescriptionRepository;
import se.inera.nll.nlllight.api.prescription.PrescriptionService;
import se.inera.nll.nlllight.api.prescription.dto.PrescriptionDTO;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patient", description = "Patient-facing API for prescriptions and medications")
public class PatientController {
    
    private final PrescriptionService prescriptionService;
    private final PatientRepository patientRepository;
    private final MedicationRepository medicationRepository;
    private final PrescriptionRepository prescriptionRepository;
    
    public PatientController(PrescriptionService prescriptionService,
                            PatientRepository patientRepository,
                            MedicationRepository medicationRepository,
                            PrescriptionRepository prescriptionRepository) {
        this.prescriptionService = prescriptionService;
        this.patientRepository = patientRepository;
        this.medicationRepository = medicationRepository;
        this.prescriptionRepository = prescriptionRepository;
    }
    
    @GetMapping("/{userId}/prescriptions")
    @Operation(summary = "Get prescriptions for a patient", 
               description = "Returns all prescriptions for the specified patient")
    public ResponseEntity<List<PrescriptionDTO>> getPatientPrescriptions(
            @PathVariable String userId,
            @RequestParam(required = false) String status) {
        
        // Verify patient exists
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + userId));
        
        List<PrescriptionDTO> prescriptions = prescriptionService.getPatientPrescriptions(patient.getId(), status);
        return ResponseEntity.ok(prescriptions);
    }
    
    @GetMapping("/{userId}/prescriptions/{id}")
    @Operation(summary = "Get prescription details", 
               description = "Returns detailed information about a specific prescription for the patient")
    public ResponseEntity<PrescriptionDTO> getPrescriptionDetails(
            @PathVariable String userId,
            @PathVariable Long id) {
        
        // Verify patient exists
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + userId));
        
        // Get the prescription entity to check ownership
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found: " + id));
        
        // Verify the prescription belongs to this patient
        if (!prescription.getPatient().getId().equals(patient.getId())) {
            throw new SecurityException("User not authorized to access this prescription");
        }
        
        // Get the prescription DTO
        PrescriptionDTO prescriptionDTO = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(prescriptionDTO);
    }
    
    @GetMapping("/{userId}/medications/{id}")
    @Operation(summary = "Get medication details", 
               description = "Returns detailed information about a specific medication")
    public ResponseEntity<Medication> getMedicationById(
            @PathVariable String userId,
            @PathVariable Long id) {
        
        // Verify patient exists
        patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + userId));
        
        // Get the medication
        Medication medication = medicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medication not found: " + id));
        
        return ResponseEntity.ok(medication);
    }
}
