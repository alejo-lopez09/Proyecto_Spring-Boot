package com.proyecto.spring.business;

import com.proyecto.spring.domain.BookingStatus;
import com.proyecto.spring.presentation.dto.ApiDtos.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {
    BookingResponse create(BookingCreateRequest request);
    BookingResponse get(Long id);
    List<BookingResponse> search(Long memberId, Long roomId, LocalDate date, BookingStatus status);
    BookingResponse addService(Long id, BookingItemRequest request);
    BookingResponse removeService(Long id, Long serviceId);
    BookingResponse confirm(Long id);
    BookingResponse cancel(Long id);
    BookingResponse complete(Long id);
    List<AvailabilityResponse> availability(Long locationId, LocalDate date, LocalDateTime start, LocalDateTime end,
                                             Integer minCapacity, List<Long> equipmentIds);
}
