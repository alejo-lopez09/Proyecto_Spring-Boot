package com.proyecto.spring.persistence;

import com.proyecto.spring.domain.Booking;
import com.proyecto.spring.domain.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("select count(b) > 0 from Booking b where b.room.id = :roomId and b.status in (com.proyecto.spring.domain.BookingStatus.PENDING, com.proyecto.spring.domain.BookingStatus.CONFIRMED) and b.startTime < :endTime and b.endTime > :startTime")
    boolean hasOverlappingBooking(@Param("roomId") Long roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    long countByMemberIdAndStatusInAndEndTimeAfter(Long memberId, List<BookingStatus> statuses, LocalDateTime now);
    List<Booking> findByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusIn(
            Long memberId, LocalDateTime from, LocalDateTime to, List<BookingStatus> statuses);
    @Query("select coalesce(sum(function('timestampdiff', minute, b.startTime, b.endTime)), 0) from Booking b where b.member.id = :memberId and b.status in :statuses and b.startTime >= :from and b.startTime < :to")
    Long sumMinutesForMemberInPeriod(@Param("memberId") Long memberId, @Param("statuses") List<BookingStatus> statuses,
                                    @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @Query("select coalesce(sum(b.roomCost), 0) from Booking b where b.member.id = :memberId and b.status in :statuses and b.startTime >= :from and b.startTime < :to")
    BigDecimal sumRoomCostsForMemberInPeriod(@Param("memberId") Long memberId, @Param("statuses") List<BookingStatus> statuses,
                                             @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    @EntityGraph(attributePaths = {"room", "room.location", "room.equipment", "member", "items", "items.service"})
    @Query("select distinct b from Booking b where b.id = :id")
    Optional<Booking> findDetailedById(@Param("id") Long id);
    @EntityGraph(attributePaths = {"room", "room.location", "member", "items", "items.service"})
    @Query("select distinct b from Booking b where (:memberId is null or b.member.id = :memberId) and (:roomId is null or b.room.id = :roomId) and (:date is null or (b.startTime >= :dayStart and b.startTime < :dayEnd)) and (:status is null or b.status = :status) order by b.startTime")
    List<Booking> search(@Param("memberId") Long memberId, @Param("roomId") Long roomId, @Param("date") LocalDate date,
                         @Param("dayStart") LocalDateTime dayStart, @Param("dayEnd") LocalDateTime dayEnd, @Param("status") BookingStatus status);
    boolean existsByRoomIdAndStatusInAndStartTimeAfter(Long roomId, List<BookingStatus> statuses, LocalDateTime after);
    boolean existsByRoomId(Long roomId);
    boolean existsByMemberId(Long memberId);
    long countByMemberIdAndStatusInAndStartTimeGreaterThanEqualAndStartTimeLessThan(Long memberId, List<BookingStatus> statuses, LocalDateTime from, LocalDateTime to);
}
