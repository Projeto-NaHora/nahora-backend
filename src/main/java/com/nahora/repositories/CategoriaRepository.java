package com.nahora.repositories;

import com.nahora.model.Categoria;
import com.nahora.model.enums.CategoriaServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    List<Categoria> findAllByOrderByNomeAsc();
    Optional<Categoria> findByCategoriaServico(CategoriaServico categoriaServico);
}
