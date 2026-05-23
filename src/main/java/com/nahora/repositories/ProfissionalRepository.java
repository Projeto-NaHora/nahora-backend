package com.nahora.repositories;

import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    boolean existsByCpf(String cpf);
    Optional<Profissional> findByEmail(String email);

    List<Profissional> findByCategoriaAndAtivoTrueAndPerfilCompletoTrue(CategoriaServico categoria);
}