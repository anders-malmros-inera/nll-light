package se.inera.nll.nlllight.api.adherence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.inera.nll.nlllight.api.common.AdherenceStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdherenceRecordRepository extends JpaRepository<AdherenceRecord, Long> {
    
    List<AdherenceRecord> findByPrescriptionId(Long prescriptionId);
    
    List<AdherenceRecord> findByPatientId(String patientId);
    
    @Query("SELECT a FROM AdherenceRecord a WHERE a.prescription.id = :prescriptionId " +
           "AND a.scheduledTime BETWEEN :startDate AND :endDate ORDER BY a.scheduledTime DESC")
    List<AdherenceRecord> findByPrescriptionAndDateRange(@Param("prescriptionId") Long prescriptionId,
                                                          @Param("startDate") LocalDateTime startDate,
                                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AdherenceRecord a WHERE a.patient.id = :patientId " +
           "AND a.scheduledTime BETWEEN :startTime AND :endTime AND a.status = 'TAKEN'")
    List<AdherenceRecord> findTakenMedicationsByPatientAndTimeRange(@Param("patientId") String patientId,
                                                                     @Param("startTime") LocalDateTime startTime,
                                                                     @Param("endTime") LocalDateTime endTime);
    
    long countByPrescriptionIdAndStatus(Long prescriptionId, AdherenceStatus status);
}
