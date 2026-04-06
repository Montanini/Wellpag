package com.wellpag.controller;

import com.wellpag.dto.AuthResponse;
import com.wellpag.dto.LoginRequest;
import com.wellpag.dto.RegisterRequest;
import com.wellpag.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar novo usuário com e-mail e senha")
    public AuthResponse registrar(@Valid @RequestBody RegisterRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login com e-mail e senha")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
