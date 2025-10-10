package se.inera.nll.nlllight.api.medication;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationRepository repository;

    public MedicationController(MedicationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Medication> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medication> get(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Medication> search(@RequestParam("name") String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }
}