package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_room_time", columnList = "room_id,start_time,end_time,status"),
        @Index(name = "idx_booking_member_time", columnList = "member_id,start_time,status")
})
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    @Column(nullable = false)
    private int attendees;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;
    @Column(name = "room_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal roomCost;
    @Column(name = "services_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal servicesCost = BigDecimal.ZERO;
    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;
    @Column(name = "cancellation_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationFee = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingItem> items = new ArrayList<>();

    protected Booking() {}
    public Booking(LocalDateTime startTime, LocalDateTime endTime, int attendees, Room room, Member member, BigDecimal roomCost) {
        this.startTime = startTime; this.endTime = endTime; this.attendees = attendees;
        this.room = room; this.member = member; this.roomCost = roomCost;
        this.servicesCost = BigDecimal.ZERO; this.totalCost = roomCost;
    }
    public void addItem(BookingItem item) { items.add(item); item.setBooking(this); recalculateTotal(); }
    public void removeItem(BookingItem item) { items.remove(item); item.setBooking(null); recalculateTotal(); }
    public void recalculateTotal() {
        servicesCost = items.stream().map(BookingItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalCost = roomCost.add(servicesCost);
    }
    public Long getId() { return id; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public int getAttendees() { return attendees; }
    public void setAttendees(int attendees) { this.attendees = attendees; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public BigDecimal getRoomCost() { return roomCost; }
    public void setRoomCost(BigDecimal roomCost) { this.roomCost = roomCost; recalculateTotal(); }
    public BigDecimal getServicesCost() { return servicesCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public BigDecimal getCancellationFee() { return cancellationFee; }
    public void setCancellationFee(BigDecimal cancellationFee) { this.cancellationFee = cancellationFee; }
    public Room getRoom() { return room; }
    public Member getMember() { return member; }
    public List<BookingItem> getItems() { return items; }
}
