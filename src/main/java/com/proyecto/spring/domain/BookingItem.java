package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_items", uniqueConstraints = @UniqueConstraint(name = "uk_booking_service", columnNames = {"booking_id", "additional_service_id"}))
public class BookingItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "applied_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal appliedUnitPrice;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "additional_service_id", nullable = false)
    private AdditionalService service;
    protected BookingItem() {}
    public BookingItem(AdditionalService service, int quantity, BigDecimal appliedUnitPrice) {
        this.service = service; this.quantity = quantity; this.appliedUnitPrice = appliedUnitPrice;
    }
    public Long getId() { return id; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getAppliedUnitPrice() { return appliedUnitPrice; }
    public void setAppliedUnitPrice(BigDecimal appliedUnitPrice) { this.appliedUnitPrice = appliedUnitPrice; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public AdditionalService getService() { return service; }
    public BigDecimal getLineTotal() { return appliedUnitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
