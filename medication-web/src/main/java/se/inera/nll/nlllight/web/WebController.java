package se.inera.nll.nlllight.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
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

        // First check user attributes - more reliable than authorities
        if (principal.getAttribute("patient-id") != null) {
            return "PATIENT";
        } else if (principal.getAttribute("prescriber-id") != null) {
            return "PRESCRIBER";
        } else if (principal.getAttribute("pharmacist-id") != null) {
            return "PHARMACIST";
        }

        // Fallback to checking authorities/roles
        for (GrantedAuthority authority : principal.getAuthorities()) {
            String role = authority.getAuthority();
            if ("PATIENT".equals(role) || "ROLE_PATIENT".equals(role)) {
                return "PATIENT";
            } else if ("PRESCRIBER".equals(role) || "ROLE_PRESCRIBER".equals(role)) {
                return "PRESCRIBER";
            } else if ("PHARMACIST".equals(role) || "ROLE_PHARMACIST".equals(role)) {
                return "PHARMACIST";
            }
        }
        System.out.println("No matching role found, authorities: " + principal.getAuthorities());
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
        String role = getUserRole(principal);
        
        if (role != null) {
            switch (role) {
                case "PATIENT":
                    return "redirect:/patient/dashboard";
                case "PRESCRIBER":
                    return "redirect:/prescriber/dashboard";
                case "PHARMACIST":
                    return "redirect:/pharmacist/dashboard";
                default:
                    return "redirect:/prescriptions"; // fallback
            }
        }
        
        // No role determined, show generic prescriptions view
        return "redirect:/prescriptions";
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
        String role = getUserRole(principal);
        String userId = null;
        PrescriptionView[] prescriptions = null;
        
        if ("PATIENT".equals(role)) {
            userId = getPatientId(principal);
            prescriptions = rest.get()
                    .uri("/api/v1/prescriptions")
                    .header("X-Patient-Id", userId)
                    .retrieve()
                    .body(PrescriptionView[].class);
        } else if ("PRESCRIBER".equals(role)) {
            userId = getPrescriberId(principal);
            // For prescribers, we might need a different endpoint or filter
            // For now, redirect to prescriber dashboard
            return "redirect:/prescriber/dashboard";
        } else if ("PHARMACIST".equals(role)) {
            userId = getPharmacistId(principal);
            // For pharmacists, we might need a different endpoint or filter
            // For now, redirect to pharmacist dashboard
            return "redirect:/pharmacist/dashboard";
        } else {
            // Fallback for unknown roles - assume patient
            userId = getPatientId(principal);
            prescriptions = rest.get()
                    .uri("/api/v1/prescriptions")
                    .header("X-Patient-Id", userId)
                    .retrieve()
                    .body(PrescriptionView[].class);
        }
        
        List<PrescriptionView> prescriptionList = prescriptions != null ? Arrays.asList(prescriptions) : List.of();
        
        model.addAttribute("prescriptions", prescriptionList);
        model.addAttribute("patientId", userId);
        model.addAttribute("userRole", role);
        
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
    @PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")
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
    @PreAuthorize("hasAnyAuthority('ROLE_PRESCRIBER', 'PRESCRIBER')")
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