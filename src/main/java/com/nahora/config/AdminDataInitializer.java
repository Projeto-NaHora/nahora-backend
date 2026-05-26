package com.nahora.config;

import com.nahora.model.Admin;
import com.nahora.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByEmail("admin@nahora.com").isPresent()) return;

        Admin admin = new Admin();
        admin.setNome("Admin NaHora");
        admin.setEmail("admin@nahora.com");
        admin.setTelefone("00000000000");
        admin.setSenha(passwordEncoder.encode(adminPassword));
        admin.setAtivo(true);
        usuarioRepository.save(admin);
    }
}
