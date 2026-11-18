package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByNameIgnoreCase(String name);
}
