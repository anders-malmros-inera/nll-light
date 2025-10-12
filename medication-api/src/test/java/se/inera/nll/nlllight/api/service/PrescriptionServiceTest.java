package se.inera.nll.nlllight.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.medication.MedicationRepository;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.patient.PatientRepository;
import se.inera.nll.nlllight.api.prescriber.Prescriber;
import se.inera.nll.nlllight.api.prescriber.PrescriberRepository;
import se.inera.nll.nlllight.api.prescription.Prescription;
import se.inera.nll.nlllight.api.prescription.PrescriptionRepository;
import se.inera.nll.nlllight.api.prescription.PrescriptionService;
import se.inera.nll.nlllight.api.prescription.dto.CreatePrescriptionRequest;
import se.inera.nll.nlllight.api.prescription.dto.PrescriptionDTO;
import se.inera.nll.nlllight.api.prescription.dto.UpdatePrescriptionRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrescriptionService Unit Tests")
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private PrescriberRepository prescriberRepository;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private Patient testPatient;
    private Medication testMedication;
    private Prescriber testPrescriber;
    private Prescription testPrescription;

    @BeforeEach
    void setUp() {
        // Setup test patient
        testPatient = new Patient();
        testPatient.setId("patient-001");
        testPatient.setUserId("user-001");
        testPatient.setEncryptedSsn("encrypted-ssn-001");
        testPatient.setFirstName("Test");
        testPatient.setLastName("Patient");
        testPatient.setDateOfBirth(LocalDate.of(1990, 1, 1));

        // Setup test medication
        testMedication = new Medication();
        testMedication.setId(1L);
        testMedication.setNplId("NPL-12345");
        testMedication.setTradeName("Paracetamol");
        testMedication.setGenericName("Acetaminophen");
        testMedication.setForm("tablet");
        testMedication.setStrength("500mg");

        // Setup test prescriber
        testPrescriber = new Prescriber();
        testPrescriber.setId(1L);
        testPrescriber.setUserId("prescriber-001");
        testPrescriber.setFirstName("Dr");
        testPrescriber.setLastName("Smith");
        testPrescriber.setSpecialty("General Medicine");

        // Setup test prescription
        testPrescription = new Prescription();
        testPrescription.setId(1L);
        testPrescription.setPrescriptionNumber("RX-TEST-001");
        testPrescription.setPatient(testPatient);
        testPrescription.setMedication(testMedication);
        testPrescription.setPrescriber(testPrescriber);
        testPrescription.setStatus(PrescriptionStatus.ACTIVE);
        testPrescription.setDose(new BigDecimal("500.00"));
        testPrescription.setDoseUnit("mg");
        testPrescription.setFrequency("TID");
        testPrescription.setFrequencyDescription("Three times daily");
        testPrescription.setRoute("PO");
        testPrescription.setPrescribedDate(LocalDate.now());
        testPrescription.setStartDate(LocalDate.now());
        testPrescription.setQuantityPrescribed(90);
        testPrescription.setQuantityUnit("tablets");
        testPrescription.setRefillsAllowed(3);
        testPrescription.setRefillsRemaining(3);
    }

    // ============= CREATE PRESCRIPTION TESTS =============

    @Test
    @DisplayName("Should create prescription successfully")
    void shouldCreatePrescriptionSuccessfully() {
        // Arrange
        CreatePrescriptionRequest request = createValidRequest();
        
        when(patientRepository.findById("patient-001")).thenReturn(Optional.of(testPatient));
        when(medicationRepository.findById(1L)).thenReturn(Optional.of(testMedication));
        when(prescriberRepository.findByUserId("prescriber-001")).thenReturn(Optional.of(testPrescriber));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        // Act
        PrescriptionDTO result = prescriptionService.createPrescription(request, "prescriber-001");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMedicationId()).isEqualTo(1L);
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Should throw exception when patient not found")
    void shouldThrowExceptionWhenPatientNotFound() {
        // Arrange
        CreatePrescriptionRequest request = createValidRequest();
        when(patientRepository.findById("patient-001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.createPrescription(request, "prescriber-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Patient not found");
        
        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when medication not found")
    void shouldThrowExceptionWhenMedicationNotFound() {
        // Arrange
        CreatePrescriptionRequest request = createValidRequest();
        when(patientRepository.findById("patient-001")).thenReturn(Optional.of(testPatient));
        when(medicationRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.createPrescription(request, "prescriber-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Medication not found");
        
        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when prescriber not found")
    void shouldThrowExceptionWhenPrescriberNotFound() {
        // Arrange
        CreatePrescriptionRequest request = createValidRequest();
        when(patientRepository.findById("patient-001")).thenReturn(Optional.of(testPatient));
        when(medicationRepository.findById(1L)).thenReturn(Optional.of(testMedication));
        when(prescriberRepository.findByUserId("prescriber-001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.createPrescription(request, "prescriber-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Prescriber not found");
        
        verify(prescriptionRepository, never()).save(any());
    }

    // ============= UPDATE PRESCRIPTION TESTS =============

    @Test
    @DisplayName("Should update prescription successfully")
    void shouldUpdatePrescriptionSuccessfully() {
        // Arrange
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setDose(new BigDecimal("1000.00"));
        request.setFrequency("BID");
        request.setModificationReason("Dosage adjustment");
        
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriberRepository.findByUserId("prescriber-001")).thenReturn(Optional.of(testPrescriber));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        // Act
        PrescriptionDTO result = prescriptionService.updatePrescription(1L, request, "prescriber-001");

        // Assert
        assertThat(result).isNotNull();
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent prescription")
    void shouldThrowExceptionWhenPrescriptionNotFound() {
        // Arrange
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setModificationReason("Update");
        when(prescriptionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.updatePrescription(999L, request, "prescriber-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Prescription not found");
    }

    @Test
    @DisplayName("Should throw exception when unauthorized prescriber tries to update")
    void shouldThrowExceptionWhenUnauthorizedPrescriberUpdates() {
        // Arrange
        Prescriber anotherPrescriber = new Prescriber();
        anotherPrescriber.setId(2L);
        anotherPrescriber.setUserId("prescriber-002");
        
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setModificationReason("Update");
        
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriberRepository.findByUserId("prescriber-002")).thenReturn(Optional.of(anotherPrescriber));

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.updatePrescription(1L, request, "prescriber-002"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("Should throw exception when updating cancelled prescription")
    void shouldThrowExceptionWhenUpdatingCancelledPrescription() {
        // Arrange
        testPrescription.setStatus(PrescriptionStatus.CANCELLED);
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setModificationReason("Update");
        
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriberRepository.findByUserId("prescriber-001")).thenReturn(Optional.of(testPrescriber));

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.updatePrescription(1L, request, "prescriber-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot modify prescription");
    }

    // ============= CANCEL PRESCRIPTION TESTS =============

    @Test
    @DisplayName("Should cancel prescription successfully")
    void shouldCancelPrescriptionSuccessfully() {
        // Arrange
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriberRepository.findByUserId("prescriber-001")).thenReturn(Optional.of(testPrescriber));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(testPrescription);

        // Act
        prescriptionService.cancelPrescription(1L, "Patient request", "prescriber-001");

        // Assert
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Should throw exception when cancelling already cancelled prescription")
    void shouldThrowExceptionWhenAlreadyCancelled() {
        // Arrange
        testPrescription.setStatus(PrescriptionStatus.CANCELLED);
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriberRepository.findByUserId("prescriber-001")).thenReturn(Optional.of(testPrescriber));

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.cancelPrescription(1L, "Reason", "prescriber-001"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("Should throw exception when unauthorized prescriber tries to cancel")
    void shouldThrowExceptionWhenUnauthorizedPrescriberCancels() {
        // Arrange
        Prescriber anotherPrescriber = new Prescriber();
        anotherPrescriber.setId(2L);
        anotherPrescriber.setUserId("prescriber-002");
        
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));
        when(prescriberRepository.findByUserId("prescriber-002")).thenReturn(Optional.of(anotherPrescriber));

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.cancelPrescription(1L, "Reason", "prescriber-002"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not authorized");
    }

    // ============= QUERY TESTS =============

    @Test
    @DisplayName("Should get patient prescriptions with status filter")
    void shouldGetPatientPrescriptionsWithStatus() {
        // Arrange
        when(prescriptionRepository.findByPatientIdAndStatus("patient-001", PrescriptionStatus.ACTIVE))
                .thenReturn(Arrays.asList(testPrescription));

        // Act
        List<PrescriptionDTO> results = prescriptionService.getPatientPrescriptions("patient-001", "ACTIVE");

        // Assert
        assertThat(results).hasSize(1);
        verify(prescriptionRepository).findByPatientIdAndStatus("patient-001", PrescriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should get all active patient prescriptions when no status provided")
    void shouldGetActivePatientPrescriptionsWithoutStatus() {
        // Arrange
        when(prescriptionRepository.findByPatientId("patient-001"))
                .thenReturn(Arrays.asList(testPrescription));

        // Act
        List<PrescriptionDTO> results = prescriptionService.getPatientPrescriptions("patient-001", null);

        // Assert
        assertThat(results).hasSize(1);
        verify(prescriptionRepository).findByPatientId("patient-001");
    }

    @Test
    @DisplayName("Should get prescription by ID")
    void shouldGetPrescriptionById() {
        // Arrange
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(testPrescription));

        // Act
        PrescriptionDTO result = prescriptionService.getPrescriptionById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when prescription not found by ID")
    void shouldThrowExceptionWhenPrescriptionNotFoundById() {
        // Arrange
        when(prescriptionRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> prescriptionService.getPrescriptionById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should get refill eligible prescriptions")
    void shouldGetRefillEligiblePrescriptions() {
        // Arrange
        when(prescriptionRepository.findRefillEligiblePrescriptions(eq("patient-001"), any(LocalDate.class)))
                .thenReturn(Arrays.asList(testPrescription));

        // Act
        List<PrescriptionDTO> results = prescriptionService.getRefillEligiblePrescriptions("patient-001");

        // Assert
        assertThat(results).hasSize(1);
        verify(prescriptionRepository).findRefillEligiblePrescriptions(eq("patient-001"), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should get prescriber prescriptions without patient filter")
    void shouldGetPrescriberPrescriptions() {
        // Arrange
        when(prescriptionRepository.findByPrescriberId("prescriber-001"))
                .thenReturn(Arrays.asList(testPrescription));

        // Act
        List<PrescriptionDTO> results = prescriptionService.getPrescriberPrescriptions("prescriber-001", null);

        // Assert
        assertThat(results).hasSize(1);
        verify(prescriptionRepository).findByPrescriberId("prescriber-001");
    }

    @Test
    @DisplayName("Should get prescriber prescriptions filtered by patient")
    void shouldGetPrescriberPrescriptionsFilteredByPatient() {
        // Arrange
        when(prescriptionRepository.findByPrescriberIdAndPatientId("prescriber-001", "patient-001"))
                .thenReturn(Arrays.asList(testPrescription));

        // Act
        List<PrescriptionDTO> results = prescriptionService.getPrescriberPrescriptions("prescriber-001", "patient-001");

        // Assert
        assertThat(results).hasSize(1);
        verify(prescriptionRepository).findByPrescriberIdAndPatientId("prescriber-001", "patient-001");
    }

    // ============= HELPER METHODS =============

    private CreatePrescriptionRequest createValidRequest() {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest();
        request.setPatientId("patient-001");
        request.setMedicationId(1L);
        request.setDose(new BigDecimal("500.00"));
        request.setDoseUnit("mg");
        request.setFrequency("TID");
        request.setFrequencyDescription("Three times daily");
        request.setRoute("PO");
        request.setStartDate(LocalDate.now());
        request.setQuantityPrescribed(90);
        request.setQuantityUnit("tablets");
        request.setDaysSupply(30);
        request.setRefillsAllowed(3);
        request.setIndication("Pain management");
        request.setInstructions("Take with food");
        return request;
    }
}
