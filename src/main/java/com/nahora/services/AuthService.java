package com.nahora.services;

import com.nahora.dto.request.RegisterClienteRequest;
import com.nahora.dto.response.AuthResponse;
import com.nahora.model.Cliente;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final StringRedisTemplate redisTemplate;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void sendOtp(String telefone) {
        String lockKey = "otp_lock:" + telefone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Telefone bloqueado. Tente novamente em 15 minutos.");
        }

        String code = String.format("%06d", new Random().nextInt(999999));

        // Salvar código no Redis com TTL de 5 minutos
        redisTemplate.opsForValue().set("otp:" + telefone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("otp_attempts:" + telefone, "0", 5, TimeUnit.MINUTES);

        // Simulação de envio via SMS
        log.info("Mock SMS -> OTP para {}: {}", telefone, code);
    }

    public void verifyOtp(String telefone, String codigo) {
        String lockKey = "otp_lock:" + telefone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Telefone bloqueado. Tente novamente em 15 minutos.");
        }

        String savedCode = redisTemplate.opsForValue().get("otp:" + telefone);
        if (savedCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código OTP expirado ou não solicitado.");
        }

        if (!savedCode.equals(codigo)) {
            Long attempts = redisTemplate.opsForValue().increment("otp_attempts:" + telefone);
            if (attempts != null && attempts >= 3) {
                // Bloqueia por 15 minutos e limpa o OTP atual
                redisTemplate.opsForValue().set(lockKey, "locked", 15, TimeUnit.MINUTES);
                redisTemplate.delete("otp:" + telefone);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas falhas. Bloqueado por 15 minutos.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código OTP inválido.");
        }

        // Sucesso na validação OTP, libera para cadastro nas próximas horas/minutos
        redisTemplate.delete("otp:" + telefone);
        redisTemplate.delete("otp_attempts:" + telefone);
        redisTemplate.opsForValue().set("phone_verified:" + telefone, "true", 30, TimeUnit.MINUTES);
    }

    @Transactional
    public AuthResponse registerCliente(RegisterClienteRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já está em uso.");
        }
        if (usuarioRepository.existsByTelefone(request.telefone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já está em uso.");
        }

        // Verifica se o telefone passou pela validação de OTP
        String verifiedStatus = redisTemplate.opsForValue().get("phone_verified:" + request.telefone());
        if (!"true".equals(verifiedStatus)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telefone não verificado ou verificação expirou.");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        cliente.setSenha(passwordEncoder.encode(request.senha()));
        cliente.setAtivo(true);

        cliente = clienteRepository.save(cliente);

        // Limpa a verificação do redis para evitar reuso imediato na mesma sessão
        redisTemplate.delete("phone_verified:" + request.telefone());

        String accessToken = jwtService.generateAccessToken(cliente);
        String refreshToken = jwtService.generateRefreshToken(cliente);

        return new AuthResponse(accessToken, refreshToken);
    }
}