package com.proyecto.spring.business;

import com.proyecto.spring.domain.MemberStatus;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import java.time.YearMonth;
import java.util.List;

public interface MemberService {
    List<MemberResponse> list(String name, MemberStatus status);
    MemberResponse get(Long id);
    MemberResponse create(MemberCreateRequest request);
    MemberResponse update(Long id, MemberUpdateRequest request);
    MemberResponse suspend(Long id);
    MemberResponse activate(Long id);
    void delete(Long id);
    ConsumptionResponse consumption(Long id, YearMonth month);
}
