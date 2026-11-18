package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
    boolean existsByNameIgnoreCase(String name);
}
