package se.inera.nll.nlllight.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Controller
public class WebController {

    private final RestClient rest;
    private final String apiBaseUrl;

    public WebController(@Value("${api.base-url:http://localhost:8081}") String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.rest = RestClient.builder().baseUrl(apiBaseUrl).build();
    }

    @GetMapping("/")
    public String index(Model model) {
        MedicationView[] meds = rest.get()
                .uri("/api/medications")
                .retrieve()
                .body(MedicationView[].class);
        List<MedicationView> medications = meds != null ? Arrays.asList(meds) : List.of();
        model.addAttribute("apiBaseUrl", apiBaseUrl);
        model.addAttribute("medications", medications);
        return "index";
    }
}