package se.inera.nll.nlllight.api.prescriber;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrescriberRepository extends JpaRepository<Prescriber, Long> {
    
    Optional<Prescriber> findByUserId(String userId);
    
    Optional<Prescriber> findByLicenseNumber(String licenseNumber);
    
    boolean existsByUserId(String userId);
}
