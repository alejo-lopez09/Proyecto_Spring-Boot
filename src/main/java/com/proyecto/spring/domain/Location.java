package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "locations")
public class Location {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 120)
    private String name;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 255)
    private String address;
    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;
    @OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
    private List<Room> rooms = new ArrayList<>();

    protected Location() {}
    public Location(String name, String city, String address, LocalTime openingTime, LocalTime closingTime) {
        this.name = name; this.city = city; this.address = address;
        this.openingTime = openingTime; this.closingTime = closingTime;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
}
