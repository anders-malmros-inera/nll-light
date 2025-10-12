package se.inera.nll.nlllight.api.adherence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.inera.nll.nlllight.api.adherence.dto.AdherenceRecordDTO;
import se.inera.nll.nlllight.api.common.AdherenceStatus;
import se.inera.nll.nlllight.api.common.RecordSource;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.patient.PatientRepository;
import se.inera.nll.nlllight.api.prescription.Prescription;
import se.inera.nll.nlllight.api.prescription.PrescriptionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdherenceService {
    
    private final AdherenceRecordRepository adherenceRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    
    public AdherenceService(AdherenceRecordRepository adherenceRecordRepository,
                           PrescriptionRepository prescriptionRepository,
                           PatientRepository patientRepository) {
        this.adherenceRecordRepository = adherenceRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
    }
    
    public AdherenceRecordDTO recordAdherence(Long prescriptionId, String patientId, 
                                             AdherenceStatus status, String notes) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found: " + prescriptionId));
        
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + patientId));
        
        // Verify prescription belongs to patient
        if (!prescription.getPatient().getId().equals(patientId)) {
            throw new RuntimeException("Prescription does not belong to patient");
        }
        
        AdherenceRecord record = new AdherenceRecord();
        record.setPrescription(prescription);
        record.setPatient(patient);
        record.setScheduledTime(LocalDateTime.now());
        record.setActualTime(LocalDateTime.now());
        record.setStatus(status);
        record.setNotes(notes);
        record.setSource(RecordSource.PATIENT_REPORTED);
        record.setDoseTaken(prescription.getDose());
        record.setDoseUnit(prescription.getDoseUnit());
        
        AdherenceRecord saved = adherenceRecordRepository.save(record);
        return toDTO(saved);
    }
    
    public List<AdherenceRecordDTO> getAdherenceHistory(Long prescriptionId) {
        List<AdherenceRecord> records = adherenceRecordRepository.findByPrescriptionId(prescriptionId);
        return records.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    private AdherenceRecordDTO toDTO(AdherenceRecord record) {
        AdherenceRecordDTO dto = new AdherenceRecordDTO();
        dto.setId(record.getId());
        dto.setPrescriptionId(record.getPrescription().getId());
        dto.setMedicationName(record.getPrescription().getMedication().getName());
        dto.setScheduledTime(record.getScheduledTime());
        dto.setActualTime(record.getActualTime());
        dto.setStatus(record.getStatus());
        dto.setDoseTaken(record.getDoseTaken());
        dto.setDoseUnit(record.getDoseUnit());
        dto.setNotes(record.getNotes());
        return dto;
    }
}
