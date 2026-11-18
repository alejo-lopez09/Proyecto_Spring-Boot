package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findAllByOrderByIdAsc();
    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findByLocationIdOrderByIdAsc(Long locationId);
    @Query("select r from Room r join fetch r.location left join fetch r.equipment where r.id = :id")
    Optional<Room> findDetailedById(@Param("id") Long id);
    boolean existsByLocationId(Long locationId);
    @Query("select count(r) > 0 from Room r join r.equipment e where e.id = :equipmentId")
    boolean isEquipmentInUse(@Param("equipmentId") Long equipmentId);
}
