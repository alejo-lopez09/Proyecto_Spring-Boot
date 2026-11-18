package com.proyecto.spring.security;

import com.proyecto.spring.domain.UserAccount;
import com.proyecto.spring.persistence.MemberRepository;
import com.proyecto.spring.persistence.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;
    private final MemberRepository members;
    public CustomUserDetailsService(UserAccountRepository users, MemberRepository members) {
        this.users = users; this.members = members;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = users.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        Long memberId = members.findByUserAccountUsername(username).map(m -> m.getId()).orElse(null);
        return new CoworkUserPrincipal(account.getUsername(), account.getPassword(), account.getRole(), memberId);
    }
}
