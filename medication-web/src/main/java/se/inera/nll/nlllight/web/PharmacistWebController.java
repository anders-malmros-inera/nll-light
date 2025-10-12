package se.inera.nll.nlllight.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/pharmacist")
@PreAuthorize("hasAnyAuthority('ROLE_PHARMACIST', 'PHARMACIST')")
public class PharmacistWebController {

    private final RestClient rest;
    private final String apiBaseUrl;

    public PharmacistWebController(@Value("${api.base-url:http://localhost:8081}") String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.rest = RestClient.builder().baseUrl(apiBaseUrl).build();
    }
    
    private String getPharmacistId(OAuth2User principal) {
        if (principal != null && principal.getAttribute("pharmacist-id") != null) {
            Object attr = principal.getAttribute("pharmacist-id");
            if (attr instanceof List) {
                return ((List<?>) attr).get(0).toString();
            }
            return attr.toString();
        }
        return "pharmacist-001";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String pharmacistId = getPharmacistId(principal);
        
        // Get all medications (pharmacists need to see the full catalog)
        try {
            MedicationView[] meds = rest.get()
                    .uri("/api/medications")
                    .retrieve()
                    .body(MedicationView[].class);
            List<MedicationView> medications = meds != null ? Arrays.asList(meds) : List.of();
            model.addAttribute("medications", medications);
        } catch (Exception e) {
            model.addAttribute("medications", List.of());
            model.addAttribute("error", "Could not load medications: " + e.getMessage());
        }
        
        model.addAttribute("apiBaseUrl", apiBaseUrl);
        model.addAttribute("pharmacistId", pharmacistId);
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            String email = principal.getAttribute("email");
            model.addAttribute("userName", name != null ? name : "Pharmacist");
            model.addAttribute("userEmail", email);
            model.addAttribute("userRole", "PHARMACIST");
        }
        
        return "pharmacist-dashboard";
    }
    
    @GetMapping("/medications/{id}")
    public String medicationDetail(@PathVariable("id") Long id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        try {
            MedicationView medication = rest.get()
                    .uri("/api/medications/" + id)
                    .retrieve()
                    .body(MedicationView.class);
            
            model.addAttribute("medication", medication);
        } catch (Exception e) {
            model.addAttribute("error", "Could not load medication details: " + e.getMessage());
        }
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "Pharmacist");
            model.addAttribute("userRole", "PHARMACIST");
        }
        
        return "pharmacist-medication-detail";
    }
}
