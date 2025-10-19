package se.inera.nll.nlllight.api.prescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.medication.MedicationRepository;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.patient.PatientRepository;
import se.inera.nll.nlllight.api.prescriber.Prescriber;
import se.inera.nll.nlllight.api.prescriber.PrescriberRepository;
import se.inera.nll.nlllight.api.prescription.dto.CreatePrescriptionRequest;
import se.inera.nll.nlllight.api.prescription.dto.DispenseMedicationRequest;
import se.inera.nll.nlllight.api.prescription.dto.PrescriptionDTO;
import se.inera.nll.nlllight.api.prescription.dto.UpdatePrescriptionRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PrescriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(PrescriptionService.class);
    
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final MedicationRepository medicationRepository;
    private final PrescriberRepository prescriberRepository;
    
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                              PatientRepository patientRepository,
                              MedicationRepository medicationRepository,
                              PrescriberRepository prescriberRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.medicationRepository = medicationRepository;
        this.prescriberRepository = prescriberRepository;
    }
    
    public List<PrescriptionDTO> getPatientPrescriptions(String patientId, String status) {
        List<Prescription> prescriptions;
        
        if (status != null && !status.isEmpty()) {
            PrescriptionStatus prescriptionStatus = PrescriptionStatus.valueOf(status.toUpperCase());
            prescriptions = prescriptionRepository.findByPatientIdAndStatus(patientId, prescriptionStatus);
        } else {
            // Return all prescriptions for the patient (not just active ones)
            prescriptions = prescriptionRepository.findByPatientId(patientId);
        }
        
        return prescriptions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public PrescriptionDTO getPrescriptionById(Long id) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found: " + id));
        return toDTO(prescription);
    }
    
    public List<PrescriptionDTO> getRefillEligiblePrescriptions(String patientId) {
        LocalDate today = LocalDate.now();
        List<Prescription> prescriptions = prescriptionRepository
                .findRefillEligiblePrescriptions(patientId, today);
        
        return prescriptions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    private PrescriptionDTO toDTO(Prescription prescription) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setId(prescription.getId());
        dto.setPrescriptionNumber(prescription.getPrescriptionNumber());
        dto.setStatus(prescription.getStatus());
        
        // Medication info
        Medication medication = prescription.getMedication();
        if (medication != null) {
            dto.setMedicationId(medication.getId());
            dto.setMedicationName(medication.getName());
            dto.setMedicationStrength(medication.getStrength());
            dto.setMedicationForm(medication.getForm());
        }
        
        // Prescriber info
        Prescriber prescriber = prescription.getPrescriber();
        if (prescriber != null) {
            dto.setPrescriberName(prescriber.getFirstName() + " " + prescriber.getLastName());
            dto.setPrescriberSpecialty(prescriber.getSpecialty());
        }
        
        // Dosing
        dto.setDose(prescription.getDose());
        dto.setDoseUnit(prescription.getDoseUnit());
        dto.setFrequency(prescription.getFrequency());
        dto.setFrequencyDescription(prescription.getFrequencyDescription());
        dto.setRoute(prescription.getRoute());
        
        // Clinical
        dto.setIndication(prescription.getIndication());
        dto.setInstructions(prescription.getInstructions());
        
        // Temporal
        dto.setPrescribedDate(prescription.getPrescribedDate());
        dto.setStartDate(prescription.getStartDate());
        dto.setEndDate(prescription.getEndDate());
        
        // Refills
        dto.setRefillsAllowed(prescription.getRefillsAllowed());
        dto.setRefillsRemaining(prescription.getRefillsRemaining());
        dto.setNextRefillEligibleDate(prescription.getNextRefillEligibleDate());
        
        // Quantity
        dto.setQuantityPrescribed(prescription.getQuantityPrescribed());
        dto.setQuantityDispensed(prescription.getQuantityDispensed());
        dto.setQuantityUnit(prescription.getQuantityUnit());
        dto.setDaysSupply(prescription.getDaysSupply());
        
        // Flags
        dto.setIsPRN(prescription.getIsPRN());
        dto.setIsSubstitutionAllowed(prescription.getIsSubstitutionAllowed());
        
        return dto;
    }
    
    public PrescriptionDTO createPrescription(CreatePrescriptionRequest request, String prescriberUserId) {
        logger.info("Creating prescription for patient {} by prescriber {}", request.getPatientId(), prescriberUserId);
        
        // Validate patient exists
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        
        // Validate medication exists
        Medication medication = medicationRepository.findById(request.getMedicationId())
                .orElseThrow(() -> new RuntimeException("Medication not found"));
        
        // Validate prescriber exists
        Prescriber prescriber = prescriberRepository.findByUserId(prescriberUserId)
                .orElseThrow(() -> new RuntimeException("Prescriber not found: " + prescriberUserId));
        
        // Create prescription
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber("RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        prescription.setPatient(patient);
        prescription.setMedication(medication);
        prescription.setPrescriber(prescriber);
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        
        // Dosing
        prescription.setDose(request.getDose());
        prescription.setDoseUnit(request.getDoseUnit());
        prescription.setFrequency(request.getFrequency());
        prescription.setFrequencyDescription(request.getFrequencyDescription());
        prescription.setRoute(request.getRoute());
        
        // Clinical
        prescription.setIndication(request.getIndication());
        prescription.setInstructions(request.getInstructions());
        
        // Temporal
        prescription.setPrescribedDate(LocalDate.now());
        prescription.setStartDate(request.getStartDate());
        prescription.setEndDate(request.getEndDate());
        
        // Quantity and refills
        prescription.setQuantityPrescribed(request.getQuantityPrescribed());
        prescription.setQuantityUnit(request.getQuantityUnit());
        prescription.setDaysSupply(request.getDaysSupply());
        prescription.setRefillsAllowed(request.getRefillsAllowed() != null ? request.getRefillsAllowed() : 0);
        prescription.setRefillsRemaining(prescription.getRefillsAllowed());
        
        // Flags
        prescription.setIsPRN(request.getIsPRN());
        prescription.setIsSubstitutionAllowed(request.getIsSubstitutionAllowed());
        
        // Save
        Prescription saved = prescriptionRepository.save(prescription);
        logger.info("Created prescription {} for patient {}", saved.getPrescriptionNumber(), patient.getId());
        
        return toDTO(saved);
    }
    
    public PrescriptionDTO updatePrescription(Long id, UpdatePrescriptionRequest request, String prescriberUserId) {
        logger.info("Updating prescription {} by prescriber {}", id, prescriberUserId);
        
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found: " + id));
        
        // Get prescriber by userId
        Prescriber prescriber = prescriberRepository.findByUserId(prescriberUserId)
                .orElseThrow(() -> new RuntimeException("Prescriber not found: " + prescriberUserId));
        
        // Verify prescriber owns this prescription
        if (!prescription.getPrescriber().getId().equals(prescriber.getId())) {
            throw new RuntimeException("Prescriber not authorized to modify this prescription");
        }
        
        // Verify prescription is modifiable
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED || 
            prescription.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new RuntimeException("Cannot modify prescription with status: " + prescription.getStatus());
        }
        
        // Update modifiable fields
        if (request.getDose() != null) {
            prescription.setDose(request.getDose());
        }
        if (request.getDoseUnit() != null) {
            prescription.setDoseUnit(request.getDoseUnit());
        }
        if (request.getFrequency() != null) {
            prescription.setFrequency(request.getFrequency());
        }
        if (request.getFrequencyDescription() != null) {
            prescription.setFrequencyDescription(request.getFrequencyDescription());
        }
        if (request.getRoute() != null) {
            prescription.setRoute(request.getRoute());
        }
        if (request.getIndication() != null) {
            prescription.setIndication(request.getIndication());
        }
        if (request.getInstructions() != null) {
            prescription.setInstructions(request.getInstructions());
        }
        if (request.getEndDate() != null) {
            prescription.setEndDate(request.getEndDate());
        }
        if (request.getRefillsAllowed() != null) {
            prescription.setRefillsAllowed(request.getRefillsAllowed());
        }
        if (request.getIsSubstitutionAllowed() != null) {
            prescription.setIsSubstitutionAllowed(request.getIsSubstitutionAllowed());
        }
        
        // updatedAt is automatically set by @PreUpdate
        
        Prescription saved = prescriptionRepository.save(prescription);
        logger.info("Updated prescription {}: {}", id, request.getModificationReason());
        
        return toDTO(saved);
    }
    
    public void cancelPrescription(Long id, String reason, String prescriberUserId) {
        logger.info("Cancelling prescription {} by prescriber {}", id, prescriberUserId);
        
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found: " + id));
        
        // Get prescriber by userId
        Prescriber prescriber = prescriberRepository.findByUserId(prescriberUserId)
                .orElseThrow(() -> new RuntimeException("Prescriber not found: " + prescriberUserId));
        
        // Verify prescriber owns this prescription
        if (!prescription.getPrescriber().getId().equals(prescriber.getId())) {
            throw new RuntimeException("Prescriber not authorized to cancel this prescription");
        }
        
        // Verify prescription is cancellable
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new RuntimeException("Prescription already cancelled");
        }
        if (prescription.getStatus() == PrescriptionStatus.COMPLETED) {
            throw new RuntimeException("Cannot cancel completed prescription");
        }
        
        prescription.setStatus(PrescriptionStatus.CANCELLED);
        prescription.setCancelledAt(LocalDateTime.now());
        prescription.setCancelledBy(prescriberUserId);
        prescription.setCancellationReason(reason);
        
        prescriptionRepository.save(prescription);
        logger.info("Cancelled prescription {}: {}", id, reason);
    }
    
    public List<PrescriptionDTO> getPrescriberPrescriptions(String prescriberId, String patientId) {
        List<Prescription> prescriptions;
        
        if (patientId != null && !patientId.isEmpty()) {
            prescriptions = prescriptionRepository.findByPrescriberIdAndPatientId(prescriberId, patientId);
        } else {
            prescriptions = prescriptionRepository.findByPrescriberId(prescriberId);
        }
        
        return prescriptions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    public PrescriptionDTO dispenseMedication(DispenseMedicationRequest request, String pharmacistUserId) {
        logger.info("Dispensing medication for prescription ID: {} by pharmacist: {}", 
                   request.getPrescriptionId(), pharmacistUserId);
        
        Prescription prescription = prescriptionRepository.findById(request.getPrescriptionId())
                .orElseThrow(() -> new IllegalArgumentException("Prescription not found"));
        
        // Validate prescription status
        if (prescription.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new IllegalStateException("Can only dispense from active prescriptions");
        }
        
        // Calculate new dispensed quantity
        int currentDispensed = prescription.getQuantityDispensed() != null ? prescription.getQuantityDispensed() : 0;
        int newDispensed = currentDispensed + request.getQuantityToDispense();
        
        // Check if dispensing would exceed prescribed quantity
        if (prescription.getQuantityPrescribed() != null && newDispensed > prescription.getQuantityPrescribed()) {
            throw new IllegalStateException("Cannot dispense more than prescribed quantity");
        }
        
        // Update dispensed quantity
        prescription.setQuantityDispensed(newDispensed);
        
        // If fully dispensed, mark as completed
        if (prescription.getQuantityPrescribed() != null && newDispensed >= prescription.getQuantityPrescribed()) {
            prescription.setStatus(PrescriptionStatus.COMPLETED);
        }
        
        Prescription saved = prescriptionRepository.save(prescription);
        logger.info("Medication dispensed successfully. Prescription ID: {}, Total dispensed: {}", 
                   prescription.getId(), newDispensed);
        
        return toDTO(saved);
    }
}
