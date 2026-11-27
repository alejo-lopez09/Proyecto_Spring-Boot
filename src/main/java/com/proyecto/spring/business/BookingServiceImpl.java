package com.proyecto.spring.business;

import com.proyecto.spring.domain.*;
import com.proyecto.spring.persistence.*;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final List<BookingStatus> ACTIVE = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);
    private static final List<BookingStatus> CONSUMING = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final MemberRepository members;
    private final AdditionalServiceRepository services;
    private final LocationRepository locations;
    private final Clock clock;

    public BookingServiceImpl(BookingRepository bookings, RoomRepository rooms, MemberRepository members,
                              AdditionalServiceRepository services, LocationRepository locations, Clock clock) {
        this.bookings = bookings; this.rooms = rooms; this.members = members; this.services = services;
        this.locations = locations; this.clock = clock;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse create(BookingCreateRequest request, String username, String role) {
        Member member = requireMemberLocked(request.miembroId());
        authorizeMember(member, username, role);
        Room room = rooms.findByIdForUpdate(request.salaId()).orElseThrow(() -> BusinessException.notFound("Sala no encontrada"));
        validateBookingWindow(request.startTime(), request.endTime(), room, request.attendees());
        if (member.getStatus() != MemberStatus.ACTIVE) throw BusinessException.conflict("El miembro está suspendido", "RN-05");
        long activeFuture = bookings.countByMemberIdAndStatusInAndEndTimeAfter(member.getId(), ACTIVE, LocalDateTime.now(clock));
        if (activeFuture >= member.getPlan().getMaxActiveBookings()) throw BusinessException.conflict("El miembro alcanzó el máximo de reservas activas", "RN-06");
        if (bookings.hasOverlappingBooking(room.getId(), request.startTime(), request.endTime())) {
            throw BusinessException.conflict("La sala ya está reservada en ese horario", "RN-01");
        }
        BigDecimal roomCost = calculateRoomCost(member, room, request.startTime(), request.endTime());
        Booking booking = new Booking(request.startTime(), request.endTime(), request.attendees(), room, member, roomCost);
        if (request.servicios() != null) {
            Set<Long> added = new HashSet<>();
            for (BookingItemRequest item : request.servicios()) {
                if (!added.add(item.servicioId())) throw BusinessException.badRequest("No se puede repetir un servicio");
                AdditionalService service = services.findById(item.servicioId()).orElseThrow(() -> BusinessException.notFound("Servicio adicional no encontrado"));
                booking.addItem(new BookingItem(service, item.quantity(), service.getUnitPrice()));
            }
        }
        return dto(bookings.saveAndFlush(booking));
    }

    @Override @Transactional(readOnly = true)
    public BookingResponse get(Long id) { return dto(requireDetailed(id)); }

    @Override @Transactional(readOnly = true)
    public List<BookingResponse> search(Long memberId, Long roomId, LocalDate date, BookingStatus status, String username, String role) {
        if (memberId == null && roomId == null && date == null && status == null) throw BusinessException.badRequest("Debe indicar un filtro");
        if (role.equals("MEMBER")) {
            Long ownMemberId = members.findByUserAccountUsername(username).map(Member::getId)
                    .orElseThrow(() -> BusinessException.forbidden("La cuenta no corresponde a un miembro"));
            if (memberId != null && !memberId.equals(ownMemberId)) throw BusinessException.forbidden("No puede consultar las reservas de otro miembro");
            memberId = ownMemberId;
        }
        if (roomId != null && date == null) throw BusinessException.badRequest("La búsqueda por sala requiere fecha");
        LocalDateTime dayStart = date == null ? LocalDate.EPOCH.atStartOfDay() : date.atStartOfDay();
        LocalDateTime dayEnd = date == null ? LocalDate.EPOCH.plusDays(1).atStartOfDay() : date.plusDays(1).atStartOfDay();
        return bookings.search(memberId, roomId, date, dayStart, dayEnd, status).stream().map(this::dto).toList();
    }

    @Override
    public BookingResponse addService(Long id, BookingItemRequest request) {
        Booking booking = requireDetailed(id);
        if (booking.getStatus() != BookingStatus.PENDING) throw BusinessException.conflict("Los servicios solo se modifican en reservas pendientes", "RN-10");
        if (booking.getItems().stream().anyMatch(item -> item.getService().getId().equals(request.servicioId()))) {
            throw BusinessException.conflict("El servicio ya está agregado", "RN-11");
        }
        AdditionalService service = services.findById(request.servicioId()).orElseThrow(() -> BusinessException.notFound("Servicio adicional no encontrado"));
        booking.addItem(new BookingItem(service, request.quantity(), service.getUnitPrice()));
        return dto(booking);
    }
    @Override
    public BookingResponse removeService(Long id, Long serviceId) {
        Booking booking = requireDetailed(id);
        if (booking.getStatus() != BookingStatus.PENDING) throw BusinessException.conflict("Los servicios solo se modifican en reservas pendientes", "RN-10");
        BookingItem item = booking.getItems().stream().filter(i -> i.getService().getId().equals(serviceId)).findFirst()
                .orElseThrow(() -> BusinessException.notFound("El servicio no está asociado a la reserva"));
        booking.removeItem(item);
        return dto(booking);
    }
    @Override
    public BookingResponse confirm(Long id) {
        Booking booking = requireDetailed(id);
        if (booking.getStatus() != BookingStatus.PENDING) throw BusinessException.conflict("Solo se pueden confirmar reservas pendientes", "RN-09");
        booking.setStatus(BookingStatus.CONFIRMED);
        return dto(booking);
    }
    @Override
    public BookingResponse cancel(Long id) {
        Booking booking = requireDetailed(id);
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw BusinessException.conflict("La reserva no se puede cancelar", "RN-09");
        }
        Duration untilStart = Duration.between(now, booking.getStartTime());
        if (untilStart.isNegative() || untilStart.compareTo(Duration.ofHours(2)) < 0) {
            throw BusinessException.conflict("La reserva no se puede cancelar con menos de 2 horas de anticipación", "RN-08");
        }
        BigDecimal fee = untilStart.compareTo(Duration.ofHours(24)) >= 0 ? BigDecimal.ZERO
                : booking.getRoomCost().multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP);
        booking.setCancellationFee(fee);
        booking.setStatus(BookingStatus.CANCELLED);
        return dto(booking);
    }
    @Override
    public BookingResponse complete(Long id) {
        Booking booking = requireDetailed(id);
        if (booking.getStatus() != BookingStatus.CONFIRMED) throw BusinessException.conflict("Solo se pueden completar reservas confirmadas", "RN-09");
        if (LocalDateTime.now(clock).isBefore(booking.getEndTime())) throw BusinessException.conflict("La reserva todavía no terminó", "RN-09");
        booking.setStatus(BookingStatus.COMPLETED);
        return dto(booking);
    }

    @Override @Transactional(readOnly = true)
    public List<AvailabilityResponse> availability(Long locationId, LocalDate date, LocalDateTime start, LocalDateTime end,
                                                   Integer minCapacity, List<Long> equipmentIds) {
        if (!locations.existsById(locationId)) throw BusinessException.notFound("Sede no encontrada");
        if (date == null || start == null || end == null || !start.toLocalDate().equals(date) || !end.toLocalDate().equals(date) || !end.isAfter(start)) {
            throw BusinessException.badRequest("Fecha y rango horario inválidos");
        }
        return rooms.findByLocationIdOrderByIdAsc(locationId).stream()
                .filter(Room::isActive)
                .filter(room -> minCapacity == null || room.getCapacity() >= minCapacity)
                .filter(room -> equipmentIds == null || room.getEquipment().stream().map(Equipment::getId).collect(Collectors.toSet()).containsAll(equipmentIds))
                .filter(room -> !bookings.hasOverlappingBooking(room.getId(), start, end))
                .map(room -> new AvailabilityResponse(room.getId(), room.getName(), room.getType(), room.getCapacity(),
                        room.getHourlyRate(), locationId, room.getLocation().getName(), room.getEquipment().stream()
                        .map(e -> new EquipmentResponse(e.getId(), e.getName())).collect(Collectors.toCollection(LinkedHashSet::new))))
                .toList();
    }

    private void validateBookingWindow(LocalDateTime start, LocalDateTime end, Room room, int attendees) {
        if (start == null || end == null || !start.toLocalDate().equals(end.toLocalDate())) throw BusinessException.conflict("La reserva debe empezar y terminar el mismo día", "RN-02");
        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        if (minutes < 30 || minutes > 480 || minutes % 30 != 0 || duration.getSeconds() != minutes * 60) {
            throw BusinessException.conflict("La duración debe ser de 30 minutos a 8 horas, en múltiplos de 30 minutos", "RN-02");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!start.isAfter(now) || start.isBefore(now.plusHours(1))) throw BusinessException.conflict("La reserva requiere al menos una hora de anticipación", "RN-03");
        if (!room.isActive()) throw BusinessException.conflict("La sala está inactiva", "RN-04");
        if (attendees > room.getCapacity()) throw BusinessException.conflict("La cantidad de asistentes supera la capacidad de la sala", "RN-04");
        Location location = room.getLocation();
        if (start.toLocalTime().isBefore(location.getOpeningTime()) || end.toLocalTime().isAfter(location.getClosingTime())) {
            throw BusinessException.conflict("El horario está fuera del horario de la sede", "RN-03");
        }
    }
    private BigDecimal calculateRoomCost(Member member, Room room, LocalDateTime start, LocalDateTime end) {
        LocalDateTime from = start.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime to = from.plusMonths(1);
        BigDecimal hoursAlreadyBooked = bookings.findByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusIn(member.getId(), from, to, CONSUMING)
                .stream().map(b -> BigDecimal.valueOf(Duration.between(b.getStartTime(), b.getEndTime()).toMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal includedRemaining = member.getPlan().getIncludedHoursPerMonth().subtract(hoursAlreadyBooked).max(BigDecimal.ZERO);
        BigDecimal bookingHours = BigDecimal.valueOf(Duration.between(start, end).toMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal overage = bookingHours.subtract(includedRemaining).max(BigDecimal.ZERO);
        return room.getHourlyRate().multiply(overage).multiply(BigDecimal.ONE.subtract(member.getPlan().getOverageDiscount()))
                .setScale(2, RoundingMode.HALF_UP);
    }
    private Member requireMemberLocked(Long id) {
        return members.findByIdForUpdate(id).orElseThrow(() -> BusinessException.notFound("Miembro no encontrado"));
    }
    private void authorizeMember(Member member, String username, String role) {
        if (role.equals("MEMBER") && (member.getUserAccount() == null || !member.getUserAccount().getUsername().equals(username))) {
            throw BusinessException.forbidden("Solo puede reservar a su propio nombre");
        }
    }
    private Booking requireDetailed(Long id) {
        return bookings.findDetailedById(id).orElseThrow(() -> BusinessException.notFound("Reserva no encontrada"));
    }
    private BookingResponse dto(Booking b) {
        return new BookingResponse(b.getId(), b.getStartTime(), b.getEndTime(), b.getAttendees(), b.getStatus(),
                b.getRoom().getId(), b.getRoom().getName(), b.getRoom().getLocation().getId(),
                b.getMember().getId(), b.getMember().getFullName(), b.getRoomCost(), b.getServicesCost(),
                b.getTotalCost(), b.getCancellationFee(), b.getItems().stream().map(item -> new BookingItemResponse(
                item.getService().getId(), item.getService().getName(), item.getQuantity(), item.getAppliedUnitPrice(),
                item.getLineTotal())).toList());
    }
}
