package se.inera.nll.nlllight.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import se.inera.nll.nlllight.config.TestSecurityConfig;
import se.inera.nll.nlllight.api.prescription.Prescription;
import se.inera.nll.nlllight.api.prescription.PrescriptionRepository;
import se.inera.nll.nlllight.api.patient.Patient;
import se.inera.nll.nlllight.api.patient.PatientRepository;
import se.inera.nll.nlllight.api.prescriber.Prescriber;
import se.inera.nll.nlllight.api.prescriber.PrescriberRepository;
import se.inera.nll.nlllight.api.medication.Medication;
import se.inera.nll.nlllight.api.medication.MedicationRepository;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PatientController.
 * Tests the patient-facing prescription API endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
@DisplayName("Patient API Integration Tests")
class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PrescriberRepository prescriberRepository;

    @Autowired
    private MedicationRepository medicationRepository;

    private Patient testPatient;
    private Prescriber testPrescriber;
    private Medication testMedication;

    @BeforeEach
    void setUp() {
        // Create test patient
        testPatient = new Patient();
        testPatient.setId("test-patient-001"); // Set ID manually as Patient uses String ID
        testPatient.setUserId("patient-001");
        testPatient.setFirstName("Test");
        testPatient.setLastName("Patient");
        testPatient.setEncryptedSsn("19800101-1234");
        testPatient.setDateOfBirth(LocalDate.of(1980, 1, 1)); // Set required field
        testPatient = patientRepository.save(testPatient);

        // Create test prescriber
        testPrescriber = new Prescriber();
        testPrescriber.setUserId("prescriber1");
        testPrescriber.setFirstName("Dr. Anna");
        testPrescriber.setLastName("Andersson");
        testPrescriber.setLicenseNumber("PSC123456");
        testPrescriber = prescriberRepository.save(testPrescriber);

        // Create test medication
        testMedication = new Medication();
        testMedication.setName("Alvedon");
        testMedication.setGenericName("Paracetamol");
        testMedication.setStrength("500mg");
        testMedication.setForm("Tablet");
        testMedication.setAtcCode("N02BE01");
        testMedication = medicationRepository.save(testMedication);
    }

    @Test
    @DisplayName("GET /api/patients/{userId}/prescriptions - Should return all prescriptions for patient")
    void testGetPatientPrescriptions_Success() throws Exception {
        // Arrange: Create test prescriptions
        Prescription prescription1 = createPrescription("RX-001", PrescriptionStatus.ACTIVE);
        Prescription prescription2 = createPrescription("RX-002", PrescriptionStatus.COMPLETED);
        prescriptionRepository.save(prescription1);
        prescriptionRepository.save(prescription2);

        // Act & Assert
        mockMvc.perform(get("/api/patients/{userId}/prescriptions", testPatient.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].prescriptionNumber", is("RX-001")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")))
                .andExpect(jsonPath("$[1].prescriptionNumber", is("RX-002")))
                .andExpect(jsonPath("$[1].status", is("COMPLETED")));
    }

    @Test
    @DisplayName("GET /api/patients/{userId}/prescriptions - Should return empty list for patient with no prescriptions")
    void testGetPatientPrescriptions_EmptyList() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/patients/{userId}/prescriptions", testPatient.getUserId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /api/patients/{userId}/prescriptions - Should return 404 for non-existent patient")
    void testGetPatientPrescriptions_PatientNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/patients/{userId}/prescriptions", "non-existent-patient"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Patient not found")));
    }

    @Test
    @DisplayName("GET /api/patients/{userId}/prescriptions/{id} - Should return prescription details")
    void testGetPrescriptionDetails_Success() throws Exception {
        // Arrange
        Prescription prescription = createPrescription("RX-003", PrescriptionStatus.ACTIVE);
        prescription = prescriptionRepository.save(prescription);

        // Act & Assert
        mockMvc.perform(get("/api/patients/{userId}/prescriptions/{id}", 
                testPatient.getUserId(), prescription.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionNumber", is("RX-003")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.medicationName", is("Alvedon")))
                .andExpect(jsonPath("$.prescriberName", is("Dr. Anna Andersson")))
                .andExpect(jsonPath("$.daysSupply", is(30)));
    }

    @Test
    @DisplayName("GET /api/patients/{userId}/prescriptions/{id} - Should return 404 for non-existent prescription")
    void testGetPrescriptionDetails_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/patients/{userId}/prescriptions/{id}", 
                testPatient.getUserId(), 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Prescription not found")));
    }

    @Test
    @DisplayName("GET /api/patients/{userId}/prescriptions/{id} - Should return 403 when accessing another patient's prescription")
    void testGetPrescriptionDetails_Forbidden() throws Exception {
        // Arrange: Create prescription for different patient
        Patient otherPatient = new Patient();
        otherPatient.setId("test-patient-002"); // Set ID manually
        otherPatient.setUserId("patient-002");
        otherPatient.setFirstName("Other");
        otherPatient.setLastName("Patient");
        otherPatient.setEncryptedSsn("19900202-5678");
        otherPatient.setDateOfBirth(LocalDate.of(1990, 2, 2)); // Set required field
        otherPatient = patientRepository.save(otherPatient);

        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber("RX-004");
        prescription.setPatient(otherPatient);
        prescription.setMedication(testMedication);
        prescription.setPrescriber(testPrescriber);
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        prescription.setDose(new BigDecimal("500"));
        prescription.setDaysSupply(30);
        prescription.setPrescribedDate(LocalDate.now());
        prescription.setStartDate(LocalDate.now()); // Set required field
        prescription.setEndDate(LocalDate.now().plusMonths(3));
        prescription = prescriptionRepository.save(prescription);

        // Act & Assert: Try to access with wrong patient ID
        mockMvc.perform(get("/api/patients/{userId}/prescriptions/{id}", 
                testPatient.getUserId(), prescription.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("not authorized")));
    }

    @Test
    @DisplayName("GET /api/medications - Should return all medications")
    void testGetAllMedications_Success() throws Exception {
        // Arrange: Additional medications are loaded from V4__Insert_medications.sql
        // Act & Assert
        mockMvc.perform(get("/api/medications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$[*].name", hasItem("Alvedon")));
    }

    @Test
    @DisplayName("GET /api/medications/{id} - Should return medication details")
    void testGetMedicationById_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/medications/{id}", testMedication.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alvedon")))
                .andExpect(jsonPath("$.genericName", is("Paracetamol")))
                .andExpect(jsonPath("$.strength", is("500mg")))
                .andExpect(jsonPath("$.form", is("Tablet")))
                .andExpect(jsonPath("$.atcCode", is("N02BE01")));
    }

    @Test
    @DisplayName("GET /api/medications/{id} - Should return 404 for non-existent medication")
    void testGetMedicationById_NotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/medications/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("Medication not found")));
    }

    // Helper method to create test prescription
    private Prescription createPrescription(String prescriptionNumber, PrescriptionStatus status) {
        Prescription prescription = new Prescription();
        prescription.setPrescriptionNumber(prescriptionNumber);
        prescription.setPatient(testPatient);
        prescription.setMedication(testMedication);
        prescription.setPrescriber(testPrescriber);
        prescription.setStatus(status);
        prescription.setDose(new BigDecimal("500"));
        prescription.setDaysSupply(30);
        prescription.setPrescribedDate(LocalDate.now());
        prescription.setStartDate(LocalDate.now());
        prescription.setEndDate(LocalDate.now().plusMonths(3));
        return prescription;
    }
}
