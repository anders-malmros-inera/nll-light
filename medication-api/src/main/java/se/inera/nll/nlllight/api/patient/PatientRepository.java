package se.inera.nll.nlllight.api.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {
    
    Optional<Patient> findByUserId(String userId);
    
    Optional<Patient> findByEncryptedSsn(String encryptedSsn);
    
    boolean existsByUserId(String userId);
}
