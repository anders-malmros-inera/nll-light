package se.inera.nll.nlllight.api.medication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
    // Search by trade name or generic name
    @Query("SELECT m FROM Medication m WHERE " +
           "LOWER(m.tradeName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(m.genericName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Medication> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);
    
    List<Medication> findByTradeNameContainingIgnoreCase(String tradeName);
    List<Medication> findByGenericNameContainingIgnoreCase(String genericName);
    List<Medication> findByNplId(String nplId);
}