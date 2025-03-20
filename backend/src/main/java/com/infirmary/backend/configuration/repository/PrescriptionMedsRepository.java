package com.infirmary.backend.configuration.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.infirmary.backend.configuration.model.PrescriptionMeds;
import com.infirmary.backend.configuration.model.Stock;

@Repository
public interface PrescriptionMedsRepository extends JpaRepository<PrescriptionMeds,UUID>{
    Boolean existsByMedicine(Stock stock);

    @Query(value = "SELECT m.medicine_name , l.location_name , count(pm) AS count " +
               "from prescription_medicine pm " +
               "JOIN stocks m on pm.medicine = m.stock_id " +
               "JOIN prescription p on p.prescription_id = pm.prescription "+
               "JOIN appointment a on a.prescription_id = p.prescription_id "+
               "JOIN locations l on l.location_id = a.location "+
               "WHERE a.date >= :startDate " +
               "GROUP BY m.medicine_name , l.location_name " +
               "LIMIT 10" , nativeQuery = true)
    List<Object[]> countPrescriptionMedsGroupByName(@Param("startDate") LocalDate startDate);

    @Query(value = "SELECT DISTINCT pm.* FROM prescription_medicine pm " +
    "JOIN prescription p ON pm.prescription = p.prescription_id " +
    "JOIN appointment a ON p.prescription_id = a.prescription_id " +
    "JOIN patient pa ON a.sap_email = pa.sap_email " +
    "WHERE pa.sap_email = :sapEmail", nativeQuery = true)
List<PrescriptionMeds> findPrescriptionMedsBySapEmail(@Param("sapEmail") String sapEmail);


}
