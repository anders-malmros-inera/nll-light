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
import se.inera.nll.nlllight.api.prescription.dto.CreatePrescriptionRequest;
import se.inera.nll.nlllight.api.prescription.dto.UpdatePrescriptionRequest;
import se.inera.nll.nlllight.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
@DisplayName("PrescriberController Integration Tests")
class PrescriberControllerIntegrationTest {

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
    private Prescriber anotherPrescriber;
    private Prescription testPrescription;

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

        // Create another prescriber
        anotherPrescriber = new Prescriber();
        anotherPrescriber.setUserId("prescriber-002");
        anotherPrescriber.setFirstName("Dr");
        anotherPrescriber.setLastName("Jones");
        anotherPrescriber.setSpecialty("Cardiology");
        anotherPrescriber.setLicenseNumber("LIC-002");
        anotherPrescriber = prescriberRepository.save(anotherPrescriber);

        // Create test prescription
        testPrescription = createPrescription();
        testPrescription = prescriptionRepository.save(testPrescription);
    }

    @Test
    @DisplayName("POST /api/v1/prescriber/prescriptions - Should create prescription successfully")
    void shouldCreatePrescriptionSuccessfully() throws Exception {
        CreatePrescriptionRequest request = createValidPrescriptionRequest();

        mockMvc.perform(post("/api/v1/prescriber/prescriptions")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.medicationId").value(testMedication.getId()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dose").value(500.00))
                .andExpect(jsonPath("$.frequency").value("TID"))
                .andExpect(jsonPath("$.prescriptionNumber").exists());
    }

    @Test
    @DisplayName("POST /api/v1/prescriber/prescriptions - Should return 400 when patient not found")
    void shouldReturn400WhenPatientNotFound() throws Exception {
        CreatePrescriptionRequest request = createValidPrescriptionRequest();
        request.setPatientId("non-existent-patient");

        mockMvc.perform(post("/api/v1/prescriber/prescriptions")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/prescriber/prescriptions - Should return 400 when medication not found")
    void shouldReturn400WhenMedicationNotFound() throws Exception {
        CreatePrescriptionRequest request = createValidPrescriptionRequest();
        request.setMedicationId(999999L);

        mockMvc.perform(post("/api/v1/prescriber/prescriptions")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/prescriber/prescriptions - Should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest();
        // Missing required fields

        mockMvc.perform(post("/api/v1/prescriber/prescriptions")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/prescriber/prescriptions/{id} - Should update prescription successfully")
    void shouldUpdatePrescriptionSuccessfully() throws Exception {
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setDose(new BigDecimal("1000.00"));
        request.setFrequency("BID");
        request.setModificationReason("Dosage adjustment based on patient feedback");

        mockMvc.perform(put("/api/v1/prescriber/prescriptions/" + testPrescription.getId())
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPrescription.getId()));
    }

    @Test
    @DisplayName("PUT /api/v1/prescriber/prescriptions/{id} - Should return 400 when validation fails")
    void shouldReturn400WhenUpdateValidationFails() throws Exception {
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        // Missing required modificationReason

        mockMvc.perform(put("/api/v1/prescriber/prescriptions/" + testPrescription.getId())
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/prescriber/prescriptions/{id} - Should return 404 when prescription not found")
    void shouldReturn404WhenUpdatingNonExistentPrescription() throws Exception {
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setDose(new BigDecimal("1000.00"));
        request.setModificationReason("Update");

        mockMvc.perform(put("/api/v1/prescriber/prescriptions/999999")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/prescriber/prescriptions/{id} - Should return 403 when unauthorized prescriber")
    void shouldReturn403WhenUnauthorizedPrescriberUpdates() throws Exception {
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest();
        request.setDose(new BigDecimal("1000.00"));
        request.setModificationReason("Unauthorized update attempt");

        mockMvc.perform(put("/api/v1/prescriber/prescriptions/" + testPrescription.getId())
                .header("X-Prescriber-Id", "prescriber-002") // Different prescriber
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/prescriber/prescriptions/{id} - Should cancel prescription successfully")
    void shouldCancelPrescriptionSuccessfully() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("reason", "Patient discontinued medication");

        mockMvc.perform(delete("/api/v1/prescriber/prescriptions/" + testPrescription.getId())
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNoContent());

        // Verify prescription was cancelled
        Prescription cancelled = prescriptionRepository.findById(testPrescription.getId()).orElseThrow();
        assert cancelled.getStatus() == PrescriptionStatus.CANCELLED;
    }

    @Test
    @DisplayName("DELETE /api/v1/prescriber/prescriptions/{id} - Should return 404 when prescription not found")
    void shouldReturn404WhenCancellingNonExistentPrescription() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("reason", "Cancellation");

        mockMvc.perform(delete("/api/v1/prescriber/prescriptions/999999")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/prescriber/prescriptions - Should return all prescriber prescriptions")
    void shouldReturnAllPrescriberPrescriptions() throws Exception {
        // Create another prescription for the same prescriber
        Prescription another = createPrescription();
        prescriptionRepository.save(another);

        mockMvc.perform(get("/api/v1/prescriber/prescriptions")
                .header("X-Prescriber-Id", "prescriber-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/v1/prescriber/prescriptions?patientId=X - Should filter by patient ID")
    void shouldFilterPrescriberPrescriptionsByPatient() throws Exception {
        mockMvc.perform(get("/api/v1/prescriber/prescriptions")
                .header("X-Prescriber-Id", "prescriber-001")
                .param("patientId", "patient-001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    @DisplayName("GET /api/v1/prescriber/prescriptions/{id} - Should return prescription details")
    void shouldReturnPrescriberPrescriptionDetails() throws Exception {
        mockMvc.perform(get("/api/v1/prescriber/prescriptions/" + testPrescription.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPrescription.getId()))
                .andExpect(jsonPath("$.prescriptionNumber").value(testPrescription.getPrescriptionNumber()));
    }

    // Helper methods

    private Prescription createPrescription() {
        Prescription prescription = new Prescription();
        prescription.setPatient(testPatient);
        prescription.setMedication(testMedication);
        prescription.setPrescriber(testPrescriber);
        prescription.setPrescriptionNumber("RX-" + UUID.randomUUID().toString().substring(0, 8));
        prescription.setStatus(PrescriptionStatus.ACTIVE);
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

    private CreatePrescriptionRequest createValidPrescriptionRequest() {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest();
        request.setPatientId(testPatient.getId());
        request.setMedicationId(testMedication.getId());
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
        request.setIsPRN(false);
        request.setIsSubstitutionAllowed(true);
        return request;
    }
}
