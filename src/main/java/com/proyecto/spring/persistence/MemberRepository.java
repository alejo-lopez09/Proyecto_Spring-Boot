package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.Member;
import com.proyecto.spring.domain.MemberStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(@Param("id") Long id);
    @EntityGraph(attributePaths = {"plan", "userAccount"})
    @Query("select m from Member m where (:name is null or lower(m.fullName) like lower(concat('%', :name, '%'))) and (:status is null or m.status = :status)")
    List<Member> search(@Param("name") String name, @Param("status") MemberStatus status);
    @EntityGraph(attributePaths = {"plan", "userAccount"})
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findDetailedById(@Param("id") Long id);
    Optional<Member> findByUserAccountUsername(String username);
    boolean existsByPlanId(Long planId);
    boolean existsByDocumentNumberIgnoreCase(String documentNumber);
    boolean existsByEmailIgnoreCase(String email);
}
