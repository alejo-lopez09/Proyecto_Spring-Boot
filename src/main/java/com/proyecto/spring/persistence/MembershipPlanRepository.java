package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {
    boolean existsByNameIgnoreCase(String name);
}
