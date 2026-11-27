package com.proyecto.spring.business;

import com.proyecto.spring.domain.*;
import com.proyecto.spring.persistence.*;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

@Service
@Transactional
public class MemberServiceImpl implements MemberService {
    private static final List<BookingStatus> CONSUMING = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
    private final MemberRepository members;
    private final MembershipPlanRepository plans;
    private final BookingRepository bookings;
    private final Clock clock;

    public MemberServiceImpl(MemberRepository members, MembershipPlanRepository plans,
                             BookingRepository bookings, Clock clock) {
        this.members = members; this.plans = plans; this.bookings = bookings; this.clock = clock;
    }
    @Override @Transactional(readOnly = true)
    public List<MemberResponse> list(String name, MemberStatus status) { return members.search(blankToNull(name), status).stream().map(this::dto).toList(); }
    @Override @Transactional(readOnly = true)
    public MemberResponse get(Long id) { return dto(require(id)); }
    @Override
    public MemberResponse create(MemberCreateRequest request) {
        if (members.existsByDocumentNumberIgnoreCase(request.documentNumber())) throw BusinessException.conflict("El documento ya existe", "RN-11");
        if (members.existsByEmailIgnoreCase(request.email())) throw BusinessException.conflict("El email ya existe", "RN-11");
        MembershipPlan plan = plans.findById(request.planId()).orElseThrow(() -> BusinessException.notFound("Plan no encontrado"));
        Member member = new Member(request.documentNumber(), request.fullName(), request.email(), plan);
        return dto(members.save(member));
    }
    @Override
    public MemberResponse update(Long id, MemberUpdateRequest request) {
        Member member = require(id);
        if (members.existsByEmailIgnoreCase(request.email()) && !request.email().equalsIgnoreCase(member.getEmail())) {
            throw BusinessException.conflict("El email ya existe", "RN-11");
        }
        member.setFullName(request.fullName()); member.setEmail(request.email());
        member.setPlan(plans.findById(request.planId()).orElseThrow(() -> BusinessException.notFound("Plan no encontrado")));
        return dto(member);
    }
    @Override
    public MemberResponse suspend(Long id) { Member member = require(id); member.setStatus(MemberStatus.SUSPENDED); return dto(member); }
    @Override
    public MemberResponse activate(Long id) { Member member = require(id); member.setStatus(MemberStatus.ACTIVE); return dto(member); }
    @Override
    public void delete(Long id) {
        Member member = require(id);
        if (bookings.existsByMemberId(id)) throw BusinessException.conflict("No se puede eliminar un miembro con reservas", "RN-12");
        members.delete(member);
    }
    @Override @Transactional(readOnly = true)
    public ConsumptionResponse consumption(Long id, YearMonth month) {
        Member member = require(id);
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        List<Booking> rows = bookings.findByMemberIdAndStartTimeGreaterThanEqualAndStartTimeLessThanAndStatusIn(id, from, to, CONSUMING);
        BigDecimal used = rows.stream().map(b -> BigDecimal.valueOf(Duration.between(b.getStartTime(), b.getEndTime()).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal included = member.getPlan().getIncludedHoursPerMonth();
        BigDecimal remaining = included.subtract(used).max(BigDecimal.ZERO);
        BigDecimal overage = used.subtract(included).max(BigDecimal.ZERO);
        return new ConsumptionResponse(id, member.getFullName(), month.toString(), included, used, remaining, overage);
    }
    private Member require(Long id) { return members.findDetailedById(id).orElseThrow(() -> BusinessException.notFound("Miembro no encontrado")); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private MemberResponse dto(Member e) {
        return new MemberResponse(e.getId(), e.getDocumentNumber(), e.getFullName(), e.getEmail(), e.getStatus(),
                e.getPlan().getId(), e.getPlan().getName());
    }
}
