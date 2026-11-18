package com.proyecto.spring.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_document", columnNames = "document_number"),
        @UniqueConstraint(name = "uk_member_email", columnNames = "email")
})
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "document_number", nullable = false, length = 40)
    private String documentNumber;
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;
    @Column(nullable = false, length = 180)
    private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membership_plan_id", nullable = false)
    private MembershipPlan plan;
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_account_id", unique = true)
    private UserAccount userAccount;
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    protected Member() {}
    public Member(String documentNumber, String fullName, String email, MembershipPlan plan) {
        this.documentNumber = documentNumber; this.fullName = fullName; this.email = email;
        this.plan = plan; this.status = MemberStatus.ACTIVE;
    }
    public Long getId() { return id; }
    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }
    public MembershipPlan getPlan() { return plan; }
    public void setPlan(MembershipPlan plan) { this.plan = plan; }
    public UserAccount getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }
}
