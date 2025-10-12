package se.inera.nll.nlllight.api.prescription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.inera.nll.nlllight.api.common.PrescriptionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    
    List<Prescription> findByPatientIdAndStatus(String patientId, PrescriptionStatus status);
    
    List<Prescription> findByPatientId(String patientId);
    
    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId AND p.status = 'ACTIVE' " +
           "AND p.nextRefillEligibleDate <= :date AND p.refillsRemaining > 0")
    List<Prescription> findRefillEligiblePrescriptions(@Param("patientId") String patientId, 
                                                       @Param("date") LocalDate date);
    
    @Query("SELECT p FROM Prescription p WHERE p.patient.id = :patientId " +
           "AND p.status = 'ACTIVE' ORDER BY p.prescribedDate DESC")
    List<Prescription> findActivePrescriptionsByPatient(@Param("patientId") String patientId);
    
    @Query("SELECT p FROM Prescription p WHERE p.prescriber.userId = :prescriberUserId " +
           "ORDER BY p.prescribedDate DESC")
    List<Prescription> findByPrescriberId(@Param("prescriberUserId") String prescriberUserId);
    
    @Query("SELECT p FROM Prescription p WHERE p.prescriber.userId = :prescriberUserId " +
           "AND p.patient.id = :patientId ORDER BY p.prescribedDate DESC")
    List<Prescription> findByPrescriberIdAndPatientId(@Param("prescriberUserId") String prescriberUserId, 
                                                       @Param("patientId") String patientId);
}
