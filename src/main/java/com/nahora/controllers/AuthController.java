package com.nahora.controllers;

import com.nahora.dto.request.CadastroEmailRequest;
import com.nahora.dto.request.RefreshTokenRequest;
import com.nahora.dto.request.CadastroNomeRequest;
import com.nahora.dto.request.CadastroSenhaRequest;
import com.nahora.dto.request.ForgotPasswordRequest;
import com.nahora.dto.request.LoginRequest;
import com.nahora.dto.request.RegisterClienteRequest;
import com.nahora.dto.request.RegisterProfissionalRequest;
import com.nahora.dto.request.ResetPasswordRequest;
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

    @PostMapping("/enviar-otp")
    @Operation(summary = "A07 — Gera e envia código OTP de 6 dígitos por SMS")
    public ResponseEntity<Void> enviarOtp(@Valid @RequestBody SendOtpRequest request) {
        authService.sendOtp(request.telefone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verificar-otp")
    @Operation(summary = "A08 — Valida o código OTP. Libera o fluxo de cadastro.")
    public ResponseEntity<Void> verificarOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.telefone(), request.codigo());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cadastro/email")
    @Operation(summary = "A09 — Salva o e-mail do profissional em cadastro")
    public ResponseEntity<Void> salvarEmail(@Valid @RequestBody CadastroEmailRequest request) {
        authService.salvarEmail(request.telefone(), request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cadastro/nome")
    @Operation(summary = "A06 — Salva nome e sobrenome do profissional em cadastro")
    public ResponseEntity<Void> salvarNome(@Valid @RequestBody CadastroNomeRequest request) {
        authService.salvarNome(request.telefone(), request.nome(), request.sobrenome());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cadastro/senha")
    @Operation(summary = "A10 — Finaliza cadastro: valida senha, cria conta, retorna JWT")
    public ResponseEntity<AuthResponse> finalizarCadastro(@Valid @RequestBody CadastroSenhaRequest request) {
        AuthResponse response = authService.finalizarCadastro(request.telefone(), request.senha(), request.confirmacaoSenha());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/cliente")
    @Operation(summary = "Cadastra um novo cliente na plataforma gerando Token JWT")
    public ResponseEntity<AuthResponse> registerCliente(@Valid @RequestBody RegisterClienteRequest request) {
        AuthResponse response = authService.registerCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/register/profissional")
    @Operation(summary = "Cadastra profissional em uma etapa (fluxo legado)")
    public ResponseEntity<AuthResponse> registerProfissional(@Valid @RequestBody RegisterProfissionalRequest request) {
        AuthResponse response = authService.registerProfissional(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica usuário por e-mail ou telefone e retorna tokens JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token usando um refresh token válido")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request.refreshToken()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Gera OTP de recuperação de senha e envia por SMS/e-mail")
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
}
