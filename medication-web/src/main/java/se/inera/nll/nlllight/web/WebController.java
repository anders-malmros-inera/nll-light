package se.inera.nll.nlllight.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    
    private String getUserRole(OAuth2User principal) {
        if (principal == null) return null;
        
        // Check authorities/roles
        for (GrantedAuthority authority : principal.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_PATIENT") || role.equals("PATIENT")) {
                return "PATIENT";
            } else if (role.equals("ROLE_PRESCRIBER") || role.equals("PRESCRIBER")) {
                return "PRESCRIBER";
            } else if (role.equals("ROLE_PHARMACIST") || role.equals("PHARMACIST")) {
                return "PHARMACIST";
            }
        }
        return null;
    }
    
    private String getPatientId(OAuth2User principal) {
        if (principal != null && principal.getAttribute("patient-id") != null) {
            Object attr = principal.getAttribute("patient-id");
            if (attr instanceof List) {
                return ((List<?>) attr).get(0).toString();
            }
            return attr.toString();
        }
        return "patient-001"; // fallback
    }
    
    private String getPrescriberId(OAuth2User principal) {
        if (principal != null && principal.getAttribute("prescriber-id") != null) {
            Object attr = principal.getAttribute("prescriber-id");
            if (attr instanceof List) {
                return ((List<?>) attr).get(0).toString();
            }
            return attr.toString();
        }
        return "prescriber-001"; // fallback
    }
    
    private String getPharmacistId(OAuth2User principal) {
        if (principal != null && principal.getAttribute("pharmacist-id") != null) {
            Object attr = principal.getAttribute("pharmacist-id");
            if (attr instanceof List) {
                return ((List<?>) attr).get(0).toString();
            }
            return attr.toString();
        }
        return "pharmacist-001"; // fallback
    }

    @GetMapping("/")
    public String index(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String userRole = getUserRole(principal);
        
        // Route to role-specific dashboard
        if ("PATIENT".equals(userRole)) {
            return "redirect:/patient/dashboard";
        } else if ("PRESCRIBER".equals(userRole)) {
            return "redirect:/prescriber/dashboard";
        } else if ("PHARMACIST".equals(userRole)) {
            return "redirect:/pharmacist/dashboard";
        }
        
        // Default fallback - show medications (pharmacist view)
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
            model.addAttribute("userRole", userRole);
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
    
    @GetMapping("/prescriptions")
    public String prescriptions(Model model, @AuthenticationPrincipal OAuth2User principal) {
        String patientId = getPatientId(principal);
        
        PrescriptionView[] prescriptions = rest.get()
                .uri("/api/v1/prescriptions")
                .header("X-Patient-Id", patientId)
                .retrieve()
                .body(PrescriptionView[].class);
        
        List<PrescriptionView> prescriptionList = prescriptions != null ? Arrays.asList(prescriptions) : List.of();
        
        model.addAttribute("prescriptions", prescriptionList);
        model.addAttribute("patientId", patientId);
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "User");
        }
        
        return "prescriptions";
    }
    
    @GetMapping("/prescriptions/{id}")
    public String prescriptionDetail(@PathVariable("id") Long id, Model model, @AuthenticationPrincipal OAuth2User principal) {
        String patientId = getPatientId(principal);
        
        PrescriptionView prescription = rest.get()
                .uri("/api/v1/prescriptions/" + id)
                .header("X-Patient-Id", patientId)
                .retrieve()
                .body(PrescriptionView.class);
        
        model.addAttribute("prescription", prescription);
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "User");
        }
        
        return "prescription-detail";
    }
    
    @GetMapping("/prescriptions/new")
    public String newPrescriptionForm(Model model, @AuthenticationPrincipal OAuth2User principal) {
        // Get list of medications for dropdown
        MedicationView[] meds = rest.get()
                .uri("/api/medications")
                .retrieve()
                .body(MedicationView[].class);
        List<MedicationView> medications = meds != null ? Arrays.asList(meds) : List.of();
        
        model.addAttribute("medications", medications);
        model.addAttribute("request", new CreatePrescriptionRequest());
        
        // Add user information
        if (principal != null) {
            String name = principal.getAttribute("name");
            model.addAttribute("userName", name != null ? name : "User");
        }
        
        return "create-prescription";
    }
    
    @PostMapping("/prescriptions")
    public String createPrescription(@ModelAttribute CreatePrescriptionRequest request, 
                                    @AuthenticationPrincipal OAuth2User principal,
                                    RedirectAttributes redirectAttributes) {
        String patientId = getPatientId(principal);
        request.setPatientId(patientId);
        
        try {
            // Note: This endpoint doesn't exist yet in the API, 
            // but we're preparing the UI for when it does
            PrescriptionView created = rest.post()
                    .uri("/api/v1/prescriptions")
                    .header("X-Patient-Id", patientId)
                    .body(request)
                    .retrieve()
                    .body(PrescriptionView.class);
            
            redirectAttributes.addFlashAttribute("success", "Recept skapat!");
            return "redirect:/prescriptions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Kunde inte skapa recept: " + e.getMessage());
            return "redirect:/prescriptions/new";
        }
    }
}