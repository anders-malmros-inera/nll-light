package se.inera.nll.nlllight.api.medication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MedicationControllerTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void list_shouldReturnSeededMedications() {
        ResponseEntity<List<Medication>> resp = rest.exchange(
                "http://localhost:" + port + "/api/medications",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Medication>>() {}
        );
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(resp.getBody()).extracting(Medication::getName)
                .contains("Alimemazin Evolan", "Elvanse", "Melatonin Orifarm");
    }

    @Test
    void search_shouldFindMelatoninByPartial() {
        ResponseEntity<List<Medication>> resp = rest.exchange(
                "http://localhost:" + port + "/api/medications/search?name=mel",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Medication>>() {}
        );
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody()).extracting(Medication::getName)
                .contains("Melatonin Orifarm");
    }

    @Test
    void get_shouldReturnSingleMedication() {
        Medication med = rest.getForObject("http://localhost:" + port + "/api/medications/1", Medication.class);
        assertThat(med).isNotNull();
        assertThat(med.getName()).isEqualTo("Alimemazin Evolan");
    }
}