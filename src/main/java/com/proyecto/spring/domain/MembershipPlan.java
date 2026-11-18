package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(name = "included_hours_per_month", nullable = false, precision = 6, scale = 2)
    private BigDecimal includedHoursPerMonth;
    @Column(name = "overage_discount", nullable = false, precision = 5, scale = 4)
    private BigDecimal overageDiscount;
    @Column(name = "max_active_bookings", nullable = false)
    private int maxActiveBookings;
    @OneToMany(mappedBy = "plan", fetch = FetchType.LAZY)
    private List<Member> members = new ArrayList<>();
    protected MembershipPlan() {}
    public MembershipPlan(String name, BigDecimal includedHoursPerMonth, BigDecimal overageDiscount, int maxActiveBookings) {
        this.name = name; this.includedHoursPerMonth = includedHoursPerMonth;
        this.overageDiscount = overageDiscount; this.maxActiveBookings = maxActiveBookings;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getIncludedHoursPerMonth() { return includedHoursPerMonth; }
    public void setIncludedHoursPerMonth(BigDecimal includedHoursPerMonth) { this.includedHoursPerMonth = includedHoursPerMonth; }
    public BigDecimal getOverageDiscount() { return overageDiscount; }
    public void setOverageDiscount(BigDecimal overageDiscount) { this.overageDiscount = overageDiscount; }
    public int getMaxActiveBookings() { return maxActiveBookings; }
    public void setMaxActiveBookings(int maxActiveBookings) { this.maxActiveBookings = maxActiveBookings; }
}
