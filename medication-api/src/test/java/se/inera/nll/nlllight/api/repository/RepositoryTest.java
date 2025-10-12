package se.inera.nll.nlllight.api.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.medication.MedicationRepository;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.patient.PatientRepository;
import se.inera.nll.nlllight.api.prescriber.Prescriber;
import se.inera.nll.nlllight.api.prescriber.PrescriberRepository;
import se.inera.nll.nlllight.api.prescription.Prescription;
import se.inera.nll.nlllight.api.prescription.PrescriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Repository Layer Tests")
class RepositoryTest {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private PrescriberRepository prescriberRepository;

    private Patient testPatient;
    private Patient anotherPatient;
    private Medication testMedication;
    private Prescriber testPrescriber;
    private Prescriber anotherPrescriber;

    @BeforeEach
    void setUp() {
        // Clean database
        prescriptionRepository.deleteAll();
        patientRepository.deleteAll();
        medicationRepository.deleteAll();
        prescriberRepository.deleteAll();

        // Create test patient
        testPatient = new Patient();
        testPatient.setId("patient-001");
        testPatient.setUserId("user-001");
        testPatient.setEncryptedSsn("encrypted-ssn-001");
        testPatient.setFirstName("Test");
        testPatient.setLastName("Patient");
        testPatient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        testPatient = patientRepository.save(testPatient);

        // Create another patient
        anotherPatient = new Patient();
        anotherPatient.setId("patient-002");
        anotherPatient.setUserId("user-002");
        anotherPatient.setEncryptedSsn("encrypted-ssn-002");
        anotherPatient.setFirstName("Another");
        anotherPatient.setLastName("Patient");
        anotherPatient.setDateOfBirth(LocalDate.of(1985, 5, 15));
        anotherPatient = patientRepository.save(anotherPatient);

        // Create test medication
        testMedication = new Medication();
        testMedication.setNplId("NPL-12345");
        testMedication.setTradeName("Paracetamol");
        testMedication.setGenericName("Acetaminophen");
        testMedication.setForm("tablet");
        testMedication.setStrength("500mg");
        testMedication.setAtcCode("N02BE01");
        testMedication.setIsAvailable(true);
        testMedication = medicationRepository.save(testMedication);

        // Create test prescriber
        testPrescriber = new Prescriber();
        testPrescriber.setUserId("prescriber-001");
        testPrescriber.setFirstName("Dr");
        testPrescriber.setLastName("Smith");
        testPrescriber.setSpecialty("General Medicine");
        testPrescriber.setLicenseNumber("LIC-001");
        testPrescriber = prescriberRepository.save(testPrescriber);

        // Create another prescriber
        anotherPrescriber = new Prescriber();
        anotherPrescriber.setUserId("prescriber-002");
        anotherPrescriber.setFirstName("Dr");
        anotherPrescriber.setLastName("Jones");
        anotherPrescriber.setSpecialty("Cardiology");
        anotherPrescriber.setLicenseNumber("LIC-002");
        anotherPrescriber = prescriberRepository.save(anotherPrescriber);
    }

    @Test
    @DisplayName("Should find prescriptions by patient ID")
    void shouldFindPrescriptionsByPatientId() {
        // Create prescriptions for test patient
        Prescription rx1 = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        Prescription rx2 = createPrescription(testPatient, PrescriptionStatus.COMPLETED);
        prescriptionRepository.save(rx1);
        prescriptionRepository.save(rx2);

        // Create prescription for another patient
        Prescription rx3 = createPrescription(anotherPatient, PrescriptionStatus.ACTIVE);
        prescriptionRepository.save(rx3);

        // Test
        List<Prescription> results = prescriptionRepository.findByPatientId("patient-001");

        // Verify
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Prescription::getPatient)
                .allMatch(p -> p.getId().equals("patient-001"));
    }

    @Test
    @DisplayName("Should find prescriptions by patient ID and status")
    void shouldFindPrescriptionsByPatientIdAndStatus() {
        // Create prescriptions with different statuses
        Prescription active1 = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        Prescription active2 = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        Prescription completed = createPrescription(testPatient, PrescriptionStatus.COMPLETED);
        prescriptionRepository.save(active1);
        prescriptionRepository.save(active2);
        prescriptionRepository.save(completed);

        // Test
        List<Prescription> activeResults = prescriptionRepository
                .findByPatientIdAndStatus("patient-001", PrescriptionStatus.ACTIVE);

        // Verify
        assertThat(activeResults).hasSize(2);
        assertThat(activeResults).allMatch(rx -> rx.getStatus() == PrescriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find active prescriptions by patient")
    void shouldFindActivePrescriptionsByPatient() {
        // Create prescriptions
        Prescription active = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        Prescription cancelled = createPrescription(testPatient, PrescriptionStatus.CANCELLED);
        prescriptionRepository.save(active);
        prescriptionRepository.save(cancelled);

        // Test
        List<Prescription> results = prescriptionRepository
                .findActivePrescriptionsByPatient("patient-001");

        // Verify
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find prescription by prescription number")
    void shouldFindByPrescriptionNumber() {
        // Create prescription
        Prescription rx = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        rx.setPrescriptionNumber("RX-TEST-001");
        prescriptionRepository.save(rx);

        // Test
        Optional<Prescription> result = prescriptionRepository
                .findByPrescriptionNumber("RX-TEST-001");

        // Verify
        assertThat(result).isPresent();
        assertThat(result.get().getPrescriptionNumber()).isEqualTo("RX-TEST-001");
    }

    @Test
    @DisplayName("Should find refill-eligible prescriptions")
    void shouldFindRefillEligiblePrescriptions() {
        // Create prescription eligible for refill
        Prescription eligible = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        eligible.setRefillsRemaining(2);
        eligible.setNextRefillEligibleDate(LocalDate.now().minusDays(1)); // Yesterday - eligible
        prescriptionRepository.save(eligible);

        // Create prescription not yet eligible
        Prescription notYetEligible = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        notYetEligible.setRefillsRemaining(1);
        notYetEligible.setNextRefillEligibleDate(LocalDate.now().plusDays(5)); // Future - not eligible
        prescriptionRepository.save(notYetEligible);

        // Test
        List<Prescription> results = prescriptionRepository
                .findRefillEligiblePrescriptions("patient-001", LocalDate.now());

        // Verify
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNextRefillEligibleDate())
                .isBeforeOrEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should find prescriptions by prescriber ID")
    void shouldFindByPrescriberId() {
        // Create prescriptions for different prescribers
        Prescription rx1 = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        rx1.setPrescriber(testPrescriber);
        Prescription rx2 = createPrescription(anotherPatient, PrescriptionStatus.ACTIVE);
        rx2.setPrescriber(testPrescriber);
        Prescription rx3 = createPrescription(testPatient, PrescriptionStatus.ACTIVE);
        rx3.setPrescriber(anotherPrescriber);
        
        prescriptionRepository.save(rx1);
        prescriptionRepository.save(rx2);
        prescriptionRepository.save(rx3);

        // Test
        List<Prescription> results = prescriptionRepository.findByPrescriberId("prescriber-001");

        // Verify
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(rx -> 
                rx.getPrescriber().getUserId().equals("prescriber-001"));
    }

    @Test
    @DisplayName("Should return empty list when no prescriptions match")
    void shouldReturnEmptyWhenNoMatch() {
        // Test with non-existent patient ID
        List<Prescription> results = prescriptionRepository.findByPatientId("non-existent");

        // Verify
        assertThat(results).isEmpty();
    }

    // Helper method to create a prescription
    private Prescription createPrescription(Patient patient, PrescriptionStatus status) {
        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setMedication(testMedication);
        prescription.setPrescriber(testPrescriber);
        prescription.setPrescriptionNumber("RX-" + UUID.randomUUID().toString().substring(0, 8));
        prescription.setStatus(status);
        prescription.setDose(new BigDecimal("500.00"));
        prescription.setDoseUnit("mg");
        prescription.setFrequency("TID");
        prescription.setFrequencyDescription("Three times daily");
        prescription.setRoute("PO");
        prescription.setPrescribedDate(LocalDate.now());
        prescription.setStartDate(LocalDate.now());
        prescription.setQuantityPrescribed(90);
        prescription.setQuantityUnit("tablets");
        prescription.setDaysSupply(30);
        prescription.setRefillsAllowed(3);
        prescription.setRefillsRemaining(3);
        prescription.setIsPRN(false);
        prescription.setIsSubstitutionAllowed(true);
        return prescription;
    }
}

