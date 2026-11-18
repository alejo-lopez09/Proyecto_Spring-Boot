package com.proyecto.spring.presentation.dto;

import com.proyecto.spring.domain.*;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class ApiDtos {
    private ApiDtos() {}
    public record LocationRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 100) String city,
                                  @NotBlank @Size(max = 255) String address, @NotNull LocalTime openingTime,
                                  @NotNull LocalTime closingTime) {}
    public record LocationResponse(Long id, String name, String city, String address, LocalTime openingTime, LocalTime closingTime) {}
    public record EquipmentRequest(@NotBlank @Size(max = 100) String name) {}
    public record EquipmentResponse(Long id, String name) {}
    public record PlanRequest(@NotBlank @Size(max = 100) String name, @NotNull @DecimalMin("0.0") BigDecimal includedHoursPerMonth,
                              @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal overageDiscount,
                              @Positive int maxActiveBookings) {}
    public record PlanResponse(Long id, String name, BigDecimal includedHoursPerMonth, BigDecimal overageDiscount, int maxActiveBookings) {}
    public record ServiceRequest(@NotBlank @Size(max = 100) String name, @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {}
    public record ServiceResponse(Long id, String name, BigDecimal unitPrice) {}
    public record RoomRequest(@NotBlank @Size(max = 100) String name, @NotNull RoomType type, @Positive int capacity,
                              @NotNull @DecimalMin("0.0") BigDecimal hourlyRate, boolean active,
                              @NotNull Long sedeId, Set<Long> equipamientoIds) {}
    public record RoomResponse(Long id, String name, RoomType type, int capacity, BigDecimal hourlyRate, boolean active,
                               Long sedeId, String sede, Set<EquipmentResponse> equipamiento) {}
    public record MemberCreateRequest(@NotBlank @Size(max = 40) String documentNumber, @NotBlank @Size(max = 150) String fullName,
                                      @NotBlank @Email @Size(max = 180) String email, @NotNull Long planId,
                                      @NotBlank @Size(max = 80) String username, @NotBlank @Size(min = 8, max = 72) String password) {}
    public record MemberUpdateRequest(@NotBlank @Size(max = 150) String fullName, @NotBlank @Email @Size(max = 180) String email,
                                      @NotNull Long planId) {}
    public record MemberResponse(Long id, String documentNumber, String fullName, String email, MemberStatus status,
                                 Long planId, String plan, String username) {}
    public record BookingItemRequest(@NotNull Long servicioId, @Positive int quantity) {}
    public record BookingCreateRequest(@NotNull Long salaId, @NotNull Long miembroId, @NotNull LocalDateTime startTime,
                                       @NotNull LocalDateTime endTime, @Positive int attendees,
                                       List<@NotNull @Valid BookingItemRequest> servicios) {}
    public record BookingItemResponse(Long servicioId, String servicio, int quantity, BigDecimal appliedUnitPrice, BigDecimal lineTotal) {}
    public record BookingResponse(Long id, LocalDateTime startTime, LocalDateTime endTime, int attendees, BookingStatus status,
                                  Long salaId, String sala, Long sedeId, Long miembroId, String miembro,
                                  BigDecimal roomCost, BigDecimal servicesCost, BigDecimal totalCost,
                                  BigDecimal cancellationFee, List<BookingItemResponse> servicios) {}
    public record AvailabilityResponse(Long salaId, String sala, RoomType type, int capacity, BigDecimal hourlyRate,
                                       Long sedeId, String sede, Set<EquipmentResponse> equipamiento) {}
    public record ConsumptionResponse(Long miembroId, String miembro, String mes, BigDecimal horasIncluidas,
                                      BigDecimal horasUsadas, BigDecimal horasRestantes, BigDecimal horasExcedentes) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record LoginResponse(String token, String tipo, long expiresInSeconds) {}
    public record ProfileResponse(String usuario, Role rol, Long miembroId) {}
    public record ErrorResponse(String error, String codigo, java.time.Instant timestamp) {}
}
