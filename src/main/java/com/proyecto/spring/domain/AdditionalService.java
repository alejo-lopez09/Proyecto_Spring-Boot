package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "additional_services")
public class AdditionalService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    private List<BookingItem> bookingItems = new ArrayList<>();
    protected AdditionalService() {}
    public AdditionalService(String name, BigDecimal unitPrice) { this.name = name; this.unitPrice = unitPrice; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
