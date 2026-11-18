package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_room_location_name", columnNames = {"location_id", "name"}))
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private RoomType type;
    @Column(nullable = false)
    private int capacity;
    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;
    @Column(nullable = false)
    private boolean active = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;
    @ManyToMany
    @JoinTable(name = "room_equipment", joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "equipment_id"))
    private Set<Equipment> equipment = new LinkedHashSet<>();
    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    protected Room() {}
    public Room(String name, RoomType type, int capacity, BigDecimal hourlyRate, boolean active, Location location) {
        this.name = name; this.type = type; this.capacity = capacity; this.hourlyRate = hourlyRate;
        this.active = active; this.location = location;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public Set<Equipment> getEquipment() { return equipment; }
    public void setEquipment(Set<Equipment> equipment) { this.equipment = equipment; }
}
