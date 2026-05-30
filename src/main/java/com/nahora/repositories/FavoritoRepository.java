package com.nahora.repositories;

import com.nahora.model.Favorito;
import com.nahora.model.enums.CategoriaServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    boolean existsByClienteIdAndProfissionalId(Long clienteId, Long profissionalId);

    Optional<Favorito> findByClienteIdAndProfissionalId(Long clienteId, Long profissionalId);

    Page<Favorito> findByClienteId(Long clienteId, Pageable pageable);

    @Query("SELECT f FROM Favorito f WHERE f.cliente.id = :clienteId AND :categoria MEMBER OF f.profissional.categoriasAtendidas")
    Page<Favorito> findByClienteIdAndCategoria(
            @Param("clienteId") Long clienteId,
            @Param("categoria") CategoriaServico categoria,
            Pageable pageable);
}
