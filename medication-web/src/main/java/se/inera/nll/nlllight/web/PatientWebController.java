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
@RequestMapping("/patient")
@PreAuthorize("hasAnyAuthority('ROLE_PATIENT', 'PATIENT')")
public class PatientWebController {

    private final RestClient rest;
    private final String apiBaseUrl;

    public PatientWebController(@Value("${api.base-url:http://localhost:8081}") String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.rest = RestClient.builder().baseUrl(apiBaseUrl).build();
    }
    
    private String getPatientId(OAuth2User principal) {
        if (principal != null && principal.getAttribute("patient-id") != null) {
            Object attr = principal.getAttribute("patient-id");
            if (attr instanceof List) {
                return ((List<?>) attr).get(0).toString();
            }
            return attr.toString();
        }
        return "patient-001";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String patientId = getPatientId(principal);
        
        // Get patient's prescriptions
        try {
            PrescriptionView[] prescriptions = rest.get()
                    .uri("/api/v1/prescriptions")
                    .header("X-Patient-Id", patientId)
                    .retrieve()
                    .body(PrescriptionView[].class);
            
            List<PrescriptionView> prescriptionList = prescriptions != null ? Arrays.asList(prescriptions) : List.of();
            model.addAttribute("prescriptions", prescriptionList);
        } catch (Exception e) {
            model.addAttribute("prescriptions", List.of());
            model.addAttribute("error", "Could not load prescriptions: " + e.getMessage());
        }
        
        model.addAttribute("patientId", patientId);
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "Patient");
            model.addAttribute("userRole", "PATIENT");
        }
        
        return "patient-dashboard";
    }
    
    @GetMapping("/prescriptions/{id}")
    public String prescriptionDetail(@PathVariable("id") Long id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        String patientId = getPatientId(principal);
        
        try {
            PrescriptionView prescription = rest.get()
                    .uri("/api/v1/prescriptions/" + id)
                    .header("X-Patient-Id", patientId)
                    .retrieve()
                    .body(PrescriptionView.class);
            
            model.addAttribute("prescription", prescription);
        } catch (Exception e) {
            model.addAttribute("error", "Could not load prescription: " + e.getMessage());
        }
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "Patient");
            model.addAttribute("userRole", "PATIENT");
        }
        
        return "patient-prescription-detail";
    }
}
