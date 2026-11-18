package com.proyecto.spring.security;

import com.proyecto.spring.persistence.BookingRepository;
import com.proyecto.spring.persistence.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@Component("accessControl")
public class AccessControl {
    private final MemberRepository members;
    private final BookingRepository bookings;
    public AccessControl(MemberRepository members, BookingRepository bookings) { this.members = members; this.bookings = bookings; }
    @Transactional(readOnly = true)
    public boolean isMember(Long memberId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CoworkUserPrincipal principal) || principal.getMemberId() == null) return false;
        return principal.getMemberId().equals(memberId);
    }
    @Transactional(readOnly = true)
    public boolean ownsBooking(Long bookingId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CoworkUserPrincipal principal) || principal.getMemberId() == null) return false;
        return bookings.findById(bookingId).map(b -> principal.getMemberId().equals(b.getMember().getId())).orElse(false);
    }
}
