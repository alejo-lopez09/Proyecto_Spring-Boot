package com.proyecto.spring.security;

import com.proyecto.spring.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

public class CoworkUserPrincipal implements UserDetails {
    private final String username;
    private final String password;
    private final Role role;
    private final Long memberId;
    public CoworkUserPrincipal(String username, String password, Role role, Long memberId) {
        this.username = username; this.password = password; this.role = role; this.memberId = memberId;
    }
    public Role getRole() { return role; }
    public Long getMemberId() { return memberId; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_" + role.name())); }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
