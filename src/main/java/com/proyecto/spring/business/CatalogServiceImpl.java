package com.proyecto.spring.business;

import com.proyecto.spring.domain.*;
import com.proyecto.spring.persistence.*;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {
    private final LocationRepository locations;
    private final RoomRepository rooms;
    private final EquipmentRepository equipment;
    private final MembershipPlanRepository plans;
    private final AdditionalServiceRepository services;
    private final BookingRepository bookings;
    private final MemberRepository members;
    private final Clock clock;

    public CatalogServiceImpl(LocationRepository locations, RoomRepository rooms, EquipmentRepository equipment,
                              MembershipPlanRepository plans, AdditionalServiceRepository services,
                              BookingRepository bookings, MemberRepository members, Clock clock) {
        this.locations = locations; this.rooms = rooms; this.equipment = equipment; this.plans = plans;
        this.services = services; this.bookings = bookings; this.members = members; this.clock = clock;
    }

    @Override @Transactional(readOnly = true)
    public List<LocationResponse> locations() { return locations.findAll().stream().map(this::dto).toList(); }
    @Override @Transactional(readOnly = true)
    public LocationResponse location(Long id) { return dto(require(locations.findById(id), "Sede")); }
    @Override
    public LocationResponse saveLocation(Long id, LocationRequest request) {
        if (!request.closingTime().isAfter(request.openingTime())) throw BusinessException.badRequest("El cierre debe ser posterior a la apertura");
        Location entity = id == null ? new Location(request.name(), request.city(), request.address(), request.openingTime(), request.closingTime()) : require(locations.findById(id), "Sede");
        entity.setName(request.name()); entity.setCity(request.city()); entity.setAddress(request.address());
        entity.setOpeningTime(request.openingTime()); entity.setClosingTime(request.closingTime());
        return dto(locations.save(entity));
    }
    @Override
    public void deleteLocation(Long id) {
        Location location = require(locations.findById(id), "Sede");
        if (rooms.existsByLocationId(id)) throw BusinessException.conflict("No se puede eliminar una sede con salas", "RN-12");
        locations.delete(location);
    }

    @Override @Transactional(readOnly = true)
    public List<RoomResponse> rooms(Long locationId) {
        return (locationId == null ? rooms.findAllByOrderByIdAsc() : rooms.findByLocationIdOrderByIdAsc(locationId)).stream().map(this::dto).toList();
    }
    @Override @Transactional(readOnly = true)
    public RoomResponse room(Long id) { return dto(require(rooms.findDetailedById(id), "Sala")); }
    @Override
    public RoomResponse saveRoom(Long id, RoomRequest request) {
        Location location = require(locations.findById(request.sedeId()), "Sede");
        Room room = id == null ? new Room(request.name(), request.type(), request.capacity(), request.hourlyRate(), request.active(), location) : require(rooms.findDetailedById(id), "Sala");
        room.setName(request.name()); room.setType(request.type()); room.setCapacity(request.capacity());
        room.setHourlyRate(request.hourlyRate()); room.setActive(request.active()); room.setLocation(location);
        Set<Equipment> assigned = request.equipamientoIds() == null ? Set.of() : request.equipamientoIds().stream()
                .map(equipmentId -> require(equipment.findById(equipmentId), "Equipamiento"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        room.setEquipment(assigned);
        return dto(rooms.save(room));
    }
    @Override
    public void deleteRoom(Long id) {
        Room room = require(rooms.findById(id), "Sala");
        if (bookings.existsByRoomId(id)) {
            throw BusinessException.conflict("No se puede eliminar una sala con reservas", "RN-12");
        }
        rooms.delete(room);
    }

    @Override @Transactional(readOnly = true)
    public List<EquipmentResponse> equipment() { return equipment.findAll().stream().map(this::dto).toList(); }
    @Override @Transactional(readOnly = true)
    public EquipmentResponse equipment(Long id) { return dto(require(equipment.findById(id), "Equipamiento")); }
    @Override
    public EquipmentResponse saveEquipment(Long id, EquipmentRequest request) {
        Equipment entity = id == null ? new Equipment(request.name()) : require(equipment.findById(id), "Equipamiento");
        entity.setName(request.name());
        return dto(equipment.save(entity));
    }
    @Override
    public void deleteEquipment(Long id) {
        Equipment item = require(equipment.findById(id), "Equipamiento");
        if (rooms.isEquipmentInUse(id)) throw BusinessException.conflict("No se puede eliminar equipamiento asignado a una sala", "RN-12");
        equipment.delete(item);
    }

    @Override @Transactional(readOnly = true)
    public List<ServiceResponse> services() { return services.findAll().stream().map(this::dto).toList(); }
    @Override @Transactional(readOnly = true)
    public ServiceResponse service(Long id) { return dto(require(services.findById(id), "Servicio adicional")); }
    @Override
    public ServiceResponse saveService(Long id, ServiceRequest request) {
        AdditionalService entity = id == null ? new AdditionalService(request.name(), request.unitPrice()) : require(services.findById(id), "Servicio adicional");
        entity.setName(request.name()); entity.setUnitPrice(request.unitPrice());
        return dto(services.save(entity));
    }
    @Override
    public void deleteService(Long id) {
        AdditionalService item = require(services.findById(id), "Servicio adicional");
        if (services.isInUse(id)) throw BusinessException.conflict("No se puede eliminar un servicio utilizado en reservas", "RN-12");
        services.delete(item);
    }

    @Override @Transactional(readOnly = true)
    public List<PlanResponse> plans() { return plans.findAll().stream().map(this::dto).toList(); }
    @Override @Transactional(readOnly = true)
    public PlanResponse plan(Long id) { return dto(require(plans.findById(id), "Plan")); }
    @Override
    public PlanResponse savePlan(Long id, PlanRequest request) {
        MembershipPlan entity = id == null ? new MembershipPlan(request.name(), request.includedHoursPerMonth(), request.overageDiscount(), request.maxActiveBookings()) : require(plans.findById(id), "Plan");
        entity.setName(request.name()); entity.setIncludedHoursPerMonth(request.includedHoursPerMonth());
        entity.setOverageDiscount(request.overageDiscount()); entity.setMaxActiveBookings(request.maxActiveBookings());
        return dto(plans.save(entity));
    }
    @Override
    public void deletePlan(Long id) {
        MembershipPlan plan = require(plans.findById(id), "Plan");
        if (members.existsByPlanId(id)) throw BusinessException.conflict("No se puede eliminar un plan con miembros", "RN-12");
        plans.delete(plan);
    }

    private List<BookingStatus> activeStatuses() { return List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED); }
    private <T> T require(Optional<T> value, String type) { return value.orElseThrow(() -> BusinessException.notFound(type + " no encontrado")); }
    private LocationResponse dto(Location e) { return new LocationResponse(e.getId(), e.getName(), e.getCity(), e.getAddress(), e.getOpeningTime(), e.getClosingTime()); }
    private EquipmentResponse dto(Equipment e) { return new EquipmentResponse(e.getId(), e.getName()); }
    private ServiceResponse dto(AdditionalService e) { return new ServiceResponse(e.getId(), e.getName(), e.getUnitPrice()); }
    private PlanResponse dto(MembershipPlan e) { return new PlanResponse(e.getId(), e.getName(), e.getIncludedHoursPerMonth(), e.getOverageDiscount(), e.getMaxActiveBookings()); }
    private RoomResponse dto(Room e) {
        return new RoomResponse(e.getId(), e.getName(), e.getType(), e.getCapacity(), e.getHourlyRate(), e.isActive(),
                e.getLocation().getId(), e.getLocation().getName(), e.getEquipment().stream().map(this::dto).collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
