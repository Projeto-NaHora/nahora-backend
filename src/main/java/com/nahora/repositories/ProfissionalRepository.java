package com.nahora.repositories;

import com.nahora.model.Profissional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    boolean existsByCpf(String cpf);
    Optional<Profissional> findByEmail(String email);
}