package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.AdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {
    boolean existsByNameIgnoreCase(String name);
    @Query("select count(i) > 0 from BookingItem i where i.service.id = :id")
    boolean isInUse(@Param("id") Long id);
}
