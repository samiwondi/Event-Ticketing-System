package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record HoldRequest(
  @NotNull UUID eventId,
  @NotBlank @Email String customerEmail,
  @NotEmpty List<UUID> seatIds
) {}
