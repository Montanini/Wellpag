package com.wellpag.auth.dto;

public record AuthResponse(
    String token,
    String nome,
    String email,
    String role
) {}
