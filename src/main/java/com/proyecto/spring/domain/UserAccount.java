package com.proyecto.spring.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "user_accounts")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80)
    private String username;
    @Column(nullable = false, length = 100)
    private String password;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Role role;
    @OneToOne(mappedBy = "userAccount", fetch = FetchType.LAZY)
    private Member member;
    protected UserAccount() {}
    public UserAccount(String username, String password, Role role) {
        this.username = username; this.password = password; this.role = role;
    }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
}
