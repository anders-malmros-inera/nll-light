package se.inera.nll.nlllight.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.medication.MedicationRepository;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.patient.PatientRepository;
import se.inera.nll.nlllight.api.prescriber.Prescriber;
import se.inera.nll.nlllight.api.prescriber.PrescriberRepository;
import se.inera.nll.nlllight.api.prescription.Prescription;
import se.inera.nll.nlllight.api.prescription.PrescriptionRepository;
import se.inera.nll.nlllight.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
@DisplayName("PrescriptionController Integration Tests (Patient API)")
class PrescriptionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private PrescriberRepository prescriberRepository;

    private Patient testPatient;
    private Medication testMedication;
    private Prescriber testPrescriber;
    private Prescription activePrescription;
    private Prescription completedPrescription;

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

        // Create active prescription
        activePrescription = createPrescription(PrescriptionStatus.ACTIVE);
        activePrescription = prescriptionRepository.save(activePrescription);

        // Create completed prescription
        completedPrescription = createPrescription(PrescriptionStatus.COMPLETED);
        completedPrescription = prescriptionRepository.save(completedPrescription);
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions - Should return all active prescriptions for patient")
    void shouldReturnAllActivePrescriptionsForPatient() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions")
                .header("X-Patient-Id", "patient-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].medicationName").value("Paracetamol"));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions?status=COMPLETED - Should filter prescriptions by status")
    void shouldFilterPrescriptionsByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions")
                .header("X-Patient-Id", "patient-001")
                .param("status", "COMPLETED")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions - Should return empty list when no prescriptions")
    void shouldReturnEmptyListWhenNoPrescriptions() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions")
                .header("X-Patient-Id", "patient-999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/{id} - Should return prescription details")
    void shouldReturnPrescriptionDetails() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions/" + activePrescription.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activePrescription.getId()))
                .andExpect(jsonPath("$.prescriptionNumber").value(activePrescription.getPrescriptionNumber()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.medicationName").value("Paracetamol"))
                .andExpect(jsonPath("$.dose").value(500.00))
                .andExpect(jsonPath("$.doseUnit").value("mg"))
                .andExpect(jsonPath("$.frequency").value("TID"));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/{id} - Should return 404 when prescription not found")
    void shouldReturn404WhenPrescriptionNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/prescriptions/999999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/refill-eligible - Should return refill eligible prescriptions")
    void shouldReturnRefillEligiblePrescriptions() throws Exception {
        // Create refill-eligible prescription
        Prescription refillEligible = createPrescription(PrescriptionStatus.ACTIVE);
        refillEligible.setRefillsRemaining(2);
        refillEligible.setNextRefillEligibleDate(LocalDate.now().minusDays(1));
        prescriptionRepository.save(refillEligible);

        mockMvc.perform(get("/api/v1/prescriptions/refill-eligible")
                .header("X-Patient-Id", "patient-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[*].status", everyItem(is("ACTIVE"))));
    }

    @Test
    @DisplayName("GET /api/v1/prescriptions/refill-eligible - Should return empty when no refills eligible")
    void shouldReturnEmptyWhenNoRefillEligible() throws Exception {
        // Set active prescription to not be refill eligible
        activePrescription.setRefillsRemaining(0);
        prescriptionRepository.save(activePrescription);

        mockMvc.perform(get("/api/v1/prescriptions/refill-eligible")
                .header("X-Patient-Id", "patient-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // Helper method to create a prescription
    private Prescription createPrescription(PrescriptionStatus status) {
        Prescription prescription = new Prescription();
        prescription.setPatient(testPatient);
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
