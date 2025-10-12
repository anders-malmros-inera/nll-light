package se.inera.nll.nlllight.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/prescriber")
@PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")
public class PrescriberWebController {

    private final RestClient rest;
    private final String apiBaseUrl;

    public PrescriberWebController(@Value("${api.base-url:http://localhost:8081}") String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.rest = RestClient.builder().baseUrl(apiBaseUrl).build();
    }
    
    private String getPrescriberId(OAuth2User principal) {
        if (principal != null && principal.getAttribute("prescriber-id") != null) {
            Object attr = principal.getAttribute("prescriber-id");
            if (attr instanceof List) {
                return ((List<?>) attr).get(0).toString();
            }
            return attr.toString();
        }
        return "prescriber-001";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String prescriberId = getPrescriberId(principal);
        
        // Get medications list for creating prescriptions
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
        
        model.addAttribute("prescriberId", prescriberId);
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "Prescriber");
            model.addAttribute("userRole", "PRESCRIBER");
        }
        
        return "prescriber-dashboard";
    }
    
    @GetMapping("/prescriptions/new")
    public String newPrescriptionForm(Model model, @AuthenticationPrincipal OAuth2User principal) {
        // Get list of medications for dropdown
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
        
        model.addAttribute("request", new CreatePrescriptionRequest());
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "Prescriber");
            model.addAttribute("userRole", "PRESCRIBER");
        }
        
        return "prescriber-create-prescription";
    }
    
    @PostMapping("/prescriptions")
    public String createPrescription(@ModelAttribute CreatePrescriptionRequest request, 
                                    @AuthenticationPrincipal OAuth2User principal,
                                    RedirectAttributes redirectAttributes) {
        String prescriberId = getPrescriberId(principal);
        
        try {
            // Call the prescriber API endpoint
            PrescriptionView created = rest.post()
                    .uri("/api/v1/prescriber/prescriptions")
                    .header("X-Prescriber-Id", prescriberId)
                    .body(request)
                    .retrieve()
                    .body(PrescriptionView.class);
            
            redirectAttributes.addFlashAttribute("success", "Prescription created successfully!");
            return "redirect:/prescriber/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Could not create prescription: " + e.getMessage());
            return "redirect:/prescriber/prescriptions/new";
        }
    }
}
