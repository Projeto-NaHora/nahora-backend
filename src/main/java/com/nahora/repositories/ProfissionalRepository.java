package com.nahora.repositories;

import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {

    boolean existsByCpf(String cpf);
    Optional<Profissional> findByEmail(String email);

    @Query("""
        SELECT DISTINCT p FROM Profissional p
        JOIN FETCH p.categoriasAtendidas c
        WHERE c = :categoria
          AND p.perfilCompleto = true
          AND p.ativo = true
        """)
    Page<Profissional> findByCategoria(@Param("categoria") CategoriaServico categoria, Pageable pageable);

    /**
     * Busca por nome do profissional ou pelo nome de exibição da categoria (case-insensitive).
     * A busca na Categoria usa o nome da tabela 'categoria' para suportar variações de acento,
     * ex.: "Marcenaria" ao invés de "MARCENARIA".
     */
    @Query("""
        SELECT DISTINCT p FROM Profissional p
        LEFT JOIN FETCH p.categoriasAtendidas c
        WHERE p.perfilCompleto = true
          AND p.ativo = true
          AND (
            LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
            OR c IN (
                SELECT cat.categoriaServico FROM Categoria cat
                WHERE LOWER(cat.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
            )
          )
        """)
    Page<Profissional> findByTermoOrdenados(@Param("termo") String termo, Pageable pageable);

    @Query(value = """
        SELECT u.id,
               ST_DistanceSphere(p.localizacao, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) / 1000.0 AS distancia_km
        FROM profissional p
        JOIN usuario u ON p.id = u.id
        WHERE p.perfil_completo = true
          AND u.ativo = true
          AND p.localizacao IS NOT NULL
          AND ST_DistanceSphere(p.localizacao, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)) <= :raioMetros
        ORDER BY distancia_km ASC
        LIMIT 10
        """, nativeQuery = true)
    List<Object[]> findSugeridosWithDistance(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("raioMetros") double raioMetros);
}
