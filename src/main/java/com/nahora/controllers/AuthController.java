package com.nahora.controllers;

import com.nahora.dto.request.ForgotPasswordRequest;
import com.nahora.dto.request.LoginRequest;
import com.nahora.dto.request.RegisterClienteRequest;
import com.nahora.dto.request.ResetPasswordRequest;
import com.nahora.dto.request.RegisterProfissionalRequest;
import com.nahora.dto.request.SendOtpRequest;
import com.nahora.dto.request.VerifyOtpRequest;
import com.nahora.dto.response.AuthResponse;
import com.nahora.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e Cadastro")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/send-otp")
    @Operation(summary = "Gera um código OTP de 6 dígitos e envia por SMS (Simulado)")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.telefone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Valida o código OTP informado. Limita tentativas.")
    public ResponseEntity<Void> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.telefone(), request.codigo());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/cliente")
    @Operation(summary = "Cadastra um novo cliente na plataforma gerando Token JWT")
    public ResponseEntity<AuthResponse> registerCliente(@Valid @RequestBody RegisterClienteRequest request) {
        AuthResponse response = authService.registerCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica usuário por e-mail ou telefone e retorna tokens JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Gera OTP de recuperação de senha e envia por SMS/e-mail (Simulado)")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.identificador());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefine a senha utilizando o código OTP de recuperação")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/register/profissional")
    @Operation(summary = "Cadastra um novo profissional na plataforma gerando Token JWT")
    public ResponseEntity<AuthResponse> registerProfissional(@Valid @RequestBody RegisterProfissionalRequest request) {
        AuthResponse response = authService.registerProfissional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
