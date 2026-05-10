package com.nahora.services;

import com.nahora.dto.request.LoginRequest;
import com.nahora.dto.request.RegisterClienteRequest;
import com.nahora.dto.request.RegisterProfissionalRequest;
import com.nahora.dto.request.ResetPasswordRequest;
import com.nahora.dto.response.AuthResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.ProfissionalRepository;
import com.nahora.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProfissionalRepository profissionalRepository;
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
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ---- sendOtp ----

    @Test
    @DisplayName("Deve disparar erro ao tentar enviar OTP para telefone bloqueado")
    void sendOtp_TelefoneBloqueado() {
        when(redisTemplate.hasKey("otp_lock:81999999999")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> authService.sendOtp("81999999999"));
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }
    
    // ---- verifyOtp ----
    
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
    
    // ---- registerCliente ----
    
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
    @DisplayName("Deve registrar profissional com sucesso se telefone estiver verificado")
    void registerProfissional_Sucesso() {
        // Arrange
        RegisterProfissionalRequest request = new RegisterProfissionalRequest(
                "Carlos Profissional", "carlos@email.com", "81988888888", "SenhaForte123",
                CategoriaServico.ELETRICA,
                "12345678901",
                List.of("Eletricista"),
                5,
                List.of("Recife"),
                "http://rg-frente.com",
                "http://rg-verso.com",
                "http://selfie.com"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(false);
        when(profissionalRepository.existsByCpf("12345678901")).thenReturn(false);
        when(valueOperations.get("phone_verified:" + request.telefone())).thenReturn("true");
        when(passwordEncoder.encode(request.senha())).thenReturn("hash");

        // Simula o salvamento no banco e gera um ID
        when(profissionalRepository.save(any(Profissional.class))).thenAnswer(invocation -> {
            Profissional p = invocation.getArgument(0);
            p.setId(2L);
            return p;
        });
        
        when(jwtService.generateAccessToken(any(Profissional.class))).thenReturn("access-token-pro");
        when(jwtService.generateRefreshToken(any(Profissional.class))).thenReturn("refresh-token-pro");

        // Act
        AuthResponse response = authService.registerProfissional(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token-pro", response.accessToken());
        assertEquals("refresh-token-pro", response.refreshToken());
        
        // Verifica se limpou o cache do redis
        verify(redisTemplate).delete("phone_verified:" + request.telefone());
        // Verifica se chamou o save
        verify(profissionalRepository).save(any(Profissional.class));
    }

    @Test
    @DisplayName("Deve bloquear registro de profissional se o e-mail já estiver em uso")
    void registerProfissional_EmailJaEmUso() {
        // Arrange
        RegisterProfissionalRequest request = new RegisterProfissionalRequest(
                "Carlos Profissional", "carlos@email.com", "81988888888", "SenhaForte123",
                CategoriaServico.ELETRICA, "12345678901", List.of("Eletricista"), 5, List.of("Recife"), "http://rg-frente.com", "http://rg-verso.com", "http://selfie.com"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> authService.registerProfissional(request));
        
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("E-mail já está em uso.", ex.getReason());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear registro de profissional se o CPF já estiver em uso")
    void registerProfissional_CpfJaEmUso() {
        RegisterProfissionalRequest request = new RegisterProfissionalRequest(
                "Carlos Profissional", "carlos@email.com", "81988888888", "SenhaForte123",
                CategoriaServico.ELETRICA, "12345678901", List.of("Eletricista"), 5, List.of("Recife"), "http://rg-frente.com", "http://rg-verso.com", "http://selfie.com"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(false);
        when(profissionalRepository.existsByCpf("12345678901")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerProfissional(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("CPF já está em uso.", ex.getReason());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve normalizar CPF com pontuação antes de verificar e salvar")
    void registerProfissional_NormalizaCpfComPontuacao() {
        RegisterProfissionalRequest request = new RegisterProfissionalRequest(
                "Carlos Profissional", "carlos@email.com", "81988888888", "SenhaForte123",
                CategoriaServico.ELETRICA, "123.456.789-01", List.of("Eletricista"), 5, List.of("Recife"), "http://rg-frente.com", "http://rg-verso.com", "http://selfie.com"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(false);
        when(profissionalRepository.existsByCpf("12345678901")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.registerProfissional(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(profissionalRepository).existsByCpf("12345678901");
    }

    @Test
    @DisplayName("Deve bloquear registro de profissional se o telefone já estiver em uso")
    void registerProfissional_TelefoneJaEmUso() {
        // Arrange
        RegisterProfissionalRequest request = new RegisterProfissionalRequest(
                "Carlos Profissional", "carlos@email.com", "81988888888", "SenhaForte123",
                CategoriaServico.ELETRICA, "12345678901", List.of("Eletricista"), 5, List.of("Recife"), "http://rg-frente.com", "http://rg-verso.com", "http://selfie.com"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(true);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> authService.registerProfissional(request));
        
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Telefone já está em uso.", ex.getReason());
        verify(profissionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear registro de profissional se o telefone não foi verificado no OTP")
    void registerProfissional_TelefoneNaoVerificado() {
        // Arrange
        RegisterProfissionalRequest request = new RegisterProfissionalRequest(
                "Carlos Profissional", "carlos@email.com", "81988888888", "SenhaForte123",
                CategoriaServico.ELETRICA, "12345678901", List.of("Eletricista"), 5, List.of("Recife"), "http://rg-frente.com", "http://rg-verso.com", "http://selfie.com"
        );

        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(usuarioRepository.existsByTelefone(request.telefone())).thenReturn(false);
        
        // Retornando null ou "false" para simular que não passou no OTP
        when(valueOperations.get("phone_verified:" + request.telefone())).thenReturn(null);

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, 
                () -> authService.registerProfissional(request));
        
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(profissionalRepository, never()).save(any());
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

    // ---- login ----

    @Test
    @DisplayName("Deve realizar login com e-mail e senha corretos")
    void login_SucessoComEmail() {
        Cliente cliente = new Cliente();
        cliente.setEmail("joao@email.com");
        cliente.setSenha("hash");
        cliente.setAtivo(true);

        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("SenhaForte123", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(cliente)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(cliente)).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("joao@email.com", "SenhaForte123"));

        assertNotNull(response);
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
    }

    @Test
    @DisplayName("Deve realizar login com telefone e senha corretos")
    void login_SucessoComTelefone() {
        Cliente cliente = new Cliente();
        cliente.setEmail("joao@email.com");
        cliente.setSenha("hash");
        cliente.setAtivo(true);

        when(usuarioRepository.findByTelefone("81999999999")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("SenhaForte123", "hash")).thenReturn(true);
        when(jwtService.generateAccessToken(cliente)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(cliente)).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("81999999999", "SenhaForte123"));

        assertEquals("access-token", response.accessToken());
    }

    @Test
    @DisplayName("Deve lançar UNAUTHORIZED quando usuário não existe")
    void login_UsuarioNaoEncontrado() {
        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("naoexiste@email.com", "qualquerSenha1")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar UNAUTHORIZED quando a senha é incorreta")
    void login_SenhaIncorreta() {
        Cliente cliente = new Cliente();
        cliente.setSenha("hash");
        cliente.setAtivo(true);

        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("errada1", "hash")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("joao@email.com", "errada1")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar FORBIDDEN quando a conta está desativada")
    void login_ContaDesativada() {
        Cliente cliente = new Cliente();
        cliente.setSenha("hash");
        cliente.setAtivo(false);

        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(cliente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("joao@email.com", "SenhaForte123")));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    // ---- forgotPassword ----

    @Test
    @DisplayName("Deve gerar OTP de recuperação com sucesso")
    void forgotPassword_Sucesso() {
        Cliente cliente = new Cliente();
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(cliente));
        when(redisTemplate.hasKey("pwd_reset_lock:joao@email.com")).thenReturn(false);

        assertDoesNotThrow(() -> authService.forgotPassword("joao@email.com"));

        verify(valueOperations).set(eq("pwd_reset_otp:joao@email.com"), anyString(), eq(5L), any());
    }

    @Test
    @DisplayName("Deve retornar silenciosamente quando usuário não existe no forgotPassword")
    void forgotPassword_UsuarioNaoEncontrado() {
        when(usuarioRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword("naoexiste@email.com"));
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Deve lançar TOO_MANY_REQUESTS quando bloqueado no forgotPassword")
    void forgotPassword_Bloqueado() {
        Cliente cliente = new Cliente();
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(cliente));
        when(redisTemplate.hasKey("pwd_reset_lock:joao@email.com")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.forgotPassword("joao@email.com"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
    }

    // ---- resetPassword ----

    @Test
    @DisplayName("Deve redefinir a senha com sucesso")
    void resetPassword_Sucesso() {
        ResetPasswordRequest request = new ResetPasswordRequest("joao@email.com", "654321", "NovaSenha1");

        when(redisTemplate.hasKey("pwd_reset_lock:joao@email.com")).thenReturn(false);
        when(valueOperations.get("pwd_reset_otp:joao@email.com")).thenReturn("654321");

        Cliente cliente = new Cliente();
        cliente.setEmail("joao@email.com");
        when(usuarioRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.encode("NovaSenha1")).thenReturn("novo-hash");

        assertDoesNotThrow(() -> authService.resetPassword(request));

        verify(usuarioRepository).save(cliente);
        verify(redisTemplate).delete("pwd_reset_otp:joao@email.com");
        verify(redisTemplate).delete("refresh_token:joao@email.com");
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando OTP de recuperação estiver expirado")
    void resetPassword_OtpExpirado() {
        ResetPasswordRequest request = new ResetPasswordRequest("joao@email.com", "654321", "NovaSenha1");

        when(redisTemplate.hasKey("pwd_reset_lock:joao@email.com")).thenReturn(false);
        when(valueOperations.get("pwd_reset_otp:joao@email.com")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST quando OTP de recuperação é inválido")
    void resetPassword_OtpInvalido() {
        ResetPasswordRequest request = new ResetPasswordRequest("joao@email.com", "000000", "NovaSenha1");

        when(redisTemplate.hasKey("pwd_reset_lock:joao@email.com")).thenReturn(false);
        when(valueOperations.get("pwd_reset_otp:joao@email.com")).thenReturn("654321");
        when(valueOperations.increment("pwd_reset_attempts:joao@email.com")).thenReturn(1L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear e lançar TOO_MANY_REQUESTS após 3 tentativas falhas no resetPassword")
    void resetPassword_MuitasTentativas() {
        ResetPasswordRequest request = new ResetPasswordRequest("joao@email.com", "000000", "NovaSenha1");

        when(redisTemplate.hasKey("pwd_reset_lock:joao@email.com")).thenReturn(false);
        when(valueOperations.get("pwd_reset_otp:joao@email.com")).thenReturn("654321");
        when(valueOperations.increment("pwd_reset_attempts:joao@email.com")).thenReturn(3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resetPassword(request));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(redisTemplate).delete("pwd_reset_otp:joao@email.com");
        verify(valueOperations).set(eq("pwd_reset_lock:joao@email.com"), eq("locked"), eq(15L), any());
    }

}


