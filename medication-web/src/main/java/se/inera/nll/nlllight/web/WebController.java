package se.inera.nll.nlllight.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String index(Model model, @AuthenticationPrincipal OAuth2User principal) {
        MedicationView[] meds = rest.get()
                .uri("/api/medications")
                .retrieve()
                .body(MedicationView[].class);
        List<MedicationView> medications = meds != null ? Arrays.asList(meds) : List.of();

        model.addAttribute("apiBaseUrl", apiBaseUrl);
        model.addAttribute("medications", medications);

        // Add user information for greeting
        if (principal != null) {
            String name = principal.getAttribute("name");
            String email = principal.getAttribute("email");
            model.addAttribute("userName", name != null ? name : "User");
            model.addAttribute("userEmail", email);
        }

        return "index";
    }

    @GetMapping("/login")
    public String login(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Inloggning misslyckades. Kontrollera dina uppgifter och försök igen.");
        }
        return "login";
    }
}