package com.nahora.services;

import com.nahora.dto.request.RegisterClienteRequest;
import com.nahora.dto.response.AuthResponse;
import com.nahora.model.Cliente;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Mock do comportamento básico do RedisTemplate
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Deve disparar erro ao tentar enviar OTP para telefone bloqueado")
    void sendOtp_TelefoneBloqueado() {
        when(redisTemplate.hasKey("otp_lock:81999999999")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.sendOtp("81999999999"));
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Deve validar OTP com sucesso e liberar o cadastro")
    void verifyOtp_Sucesso() {
        when(redisTemplate.hasKey("otp_lock:81999999999")).thenReturn(false);
        when(valueOperations.get("otp:81999999999")).thenReturn("123456");

        assertDoesNotThrow(() -> authService.verifyOtp("81999999999", "123456"));

        verify(redisTemplate).delete("otp:81999999999");
        verify(redisTemplate).delete("otp_attempts:81999999999");
        verify(valueOperations).set(eq("phone_verified:81999999999"), eq("true"), anyLong(), any());
    }

    @Test
    @DisplayName("Deve registrar cliente com sucesso se telefone estiver verificado")
    void registerCliente_Sucesso() {
        RegisterClienteRequest request = new RegisterClienteRequest(
                "João Silva", "joao@email.com", "81999999999", "SenhaForte123"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(false);
        when(valueOperations.get("phone_verified:" + request.telefone())).thenReturn("true");
        when(passwordEncoder.encode(request.senha())).thenReturn("hash");

        Cliente clienteSalvo = new Cliente();
        clienteSalvo.setId(1L);
        when(clienteRepository.save(any(Cliente.class))).thenReturn(clienteSalvo);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse response = authService.registerCliente(request);

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        verify(redisTemplate).delete("phone_verified:" + request.telefone());
    }

    @Test
    @DisplayName("Deve bloquear registro se o telefone não foi verificado no OTP")
    void registerCliente_TelefoneNaoVerificado() {
        RegisterClienteRequest request = new RegisterClienteRequest(
                "João Silva", "joao@email.com", "81999999999", "SenhaForte123"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(false);
        when(valueOperations.get("phone_verified:" + request.telefone())).thenReturn(null);

        assertThrows(ResponseStatusException.class, () -> authService.registerCliente(request));
        verify(clienteRepository, never()).save(any());
    }
}