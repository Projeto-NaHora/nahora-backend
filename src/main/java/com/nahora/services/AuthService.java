package com.nahora.services;

import com.nahora.dto.request.ForgotPasswordRequest;
import com.nahora.dto.request.LoginRequest;
import com.nahora.dto.request.RegisterClienteRequest;
import com.nahora.dto.request.RegisterProfissionalRequest;
import com.nahora.dto.request.ResetPasswordRequest;
import com.nahora.dto.response.AuthResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.model.enums.StatusVerificacao;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.ProfissionalRepository;
import com.nahora.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final ClienteRepository clienteRepository;
    private final ProfissionalRepository profissionalRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SmsService smsService;

    public void sendOtp(String telefone) {
        String lockKey = "otp_lock:" + telefone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Telefone bloqueado. Tente novamente em 15 minutos.");
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        // Salvar código no Redis com TTL de 5 minutos
        redisTemplate.opsForValue().set("otp:" + telefone, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("otp_attempts:" + telefone, "0", 5, TimeUnit.MINUTES);

        smsService.sendSms(telefone, "Seu código NaHora: " + code + ". Válido por 5 minutos.");
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

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = findByEmailOrPhone(request.identificador())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas."));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conta desativada.");
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }

        return new AuthResponse(
                jwtService.generateAccessToken(usuario),
                jwtService.generateRefreshToken(usuario),
                jwtService.extractTipoUsuario(usuario)
        );
    }

    public void forgotPassword(String identificador) {
        if (findByEmailOrPhone(identificador).isEmpty()) {
            return;
        }

        String lockKey = "pwd_reset_lock:" + identificador;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Aguarde antes de solicitar um novo código.");
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set("pwd_reset_otp:" + identificador, code, 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set("pwd_reset_attempts:" + identificador, "0", 5, TimeUnit.MINUTES);

        if (!identificador.contains("@")) {
            smsService.sendSms(identificador, "Código de recuperação NaHora: " + code + ". Válido por 5 minutos.");
        } else {
            log.info("OTP de recuperação gerado para {} (envio por e-mail não implementado)", identificador);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String identificador = request.identificador();

        String lockKey = "pwd_reset_lock:" + identificador;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Aguarde antes de tentar novamente.");
        }

        String otpKey = "pwd_reset_otp:" + identificador;
        String savedCode = redisTemplate.opsForValue().get(otpKey);
        if (savedCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código OTP expirado ou não solicitado.");
        }

        if (!savedCode.equals(request.codigo())) {
            Long attempts = redisTemplate.opsForValue().increment("pwd_reset_attempts:" + identificador);
            if (attempts != null && attempts >= 3) {
                redisTemplate.opsForValue().set(lockKey, "locked", 15, TimeUnit.MINUTES);
                redisTemplate.delete(otpKey);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Muitas tentativas falhas. Bloqueado por 15 minutos.");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código OTP inválido.");
        }

        Usuario usuario = findByEmailOrPhone(identificador)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);

        redisTemplate.delete(otpKey);
        redisTemplate.delete("pwd_reset_attempts:" + identificador);
        redisTemplate.delete("refresh_token:" + usuario.getEmail());
    }

    private Optional<Usuario> findByEmailOrPhone(String identificador) {
        if (identificador.contains("@")) {
            return usuarioRepository.findByEmail(identificador);
        }
        return usuarioRepository.findByTelefone(identificador);
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

        return new AuthResponse(accessToken, refreshToken, jwtService.extractTipoUsuario(cliente));
    }
    @Transactional
    public AuthResponse registerProfissional(RegisterProfissionalRequest request) {
        
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já está em uso.");
        }
        
        
        if(usuarioRepository.existsByTelefone(request.telefone())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Telefone já está em uso.");
        }

        String cpf = request.cpf().replaceAll("[^0-9]", "");
        if (profissionalRepository.existsByCpf(cpf)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já está em uso.");
        }

         // Verifica se o telefone passou pela validação de OTP
        String verifiedStatus = redisTemplate.opsForValue().get("phone_verified:" + request.telefone());
        if (!"true".equals(verifiedStatus)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Telefone não verificado ou verificação expirou.");
        }

        // Limpa a verificação do redis para evitar reuso imediato na mesma sessão
        redisTemplate.delete("phone_verified:" + request.telefone());


        
        Profissional profissional = new Profissional();
        profissional.setNome(request.nome());
        profissional.setEmail(request.email());
        profissional.setTelefone(request.telefone());
        profissional.setSenha(passwordEncoder.encode(request.senha()));
        profissional.setAtivo(true);
    
        // especificos de profissional
        profissional.setCpf(cpf);
        profissional.setCidade(request.cidade());
        profissional.setEstado(request.estado());
        profissional.setAreaAtuacao(request.areaAtuacao());
        profissional.setEspecialidades(request.especialidades());
        profissional.setDescricaoEspecialidades(request.descricaoEspecialidades());
        profissional.setAnosExperiencia(request.anosExperiencia());
        profissional.setCategoriasAtendidas(List.of(request.categoriaServico()));
        profissional.setRgFrenteUrl(request.rgFrenteUrl());
        profissional.setRgVersoUrl(request.rgVersoUrl());
        profissional.setSelfieUrl(request.selfieUrl());

        // o profissional vai ter os documentos enviados, mas ainda não verificados
        profissional.setStatusVerificacao(StatusVerificacao.PENDENTE);
        profissional.setDisponivel(false);
        profissional.setPerfilCompleto(false);

        profissional = profissionalRepository.save(profissional);

        String accessToken = jwtService.generateAccessToken(profissional);
        String refreshToken = jwtService.generateRefreshToken(profissional);

        return new AuthResponse(accessToken, refreshToken, jwtService.extractTipoUsuario(profissional));


    }

}