package com.nahora.services;

import com.nahora.dto.response.*;
import com.nahora.model.Categoria;
import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.CategoriaRepository;
import com.nahora.repositories.ProfissionalRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuscaService {

    private final CategoriaRepository categoriaRepository;
    private final ProfissionalRepository profissionalRepository;

    private Map<CategoriaServico, String> categoriaNames = new HashMap<>();

    @PostConstruct
    void initCategoriaCache() {
        categoriaNames = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getCategoriaServico, Categoria::getNome));
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarCategorias() {
        return categoriaRepository.findAllByOrderByNomeAsc().stream()
                .map(c -> new CategoriaDTO(c.getId(), c.getNome(), c.getIcone()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalBuscaResponse buscarPorCategoria(Long categoriaId, Pageable pageable) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));

        Page<Profissional> page = profissionalRepository.findByCategoria(
                categoria.getCategoriaServico(), pageable);

        List<ProfissionalCardDTO> cards = page.getContent().stream()
                .map(p -> toCardDTO(p, null))
                .toList();

        return new ProfissionalBuscaResponse(
                cards,
                page.getTotalElements(),
                page.getNumber(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ProfissionalBuscaResponse buscarPorTermo(String termo, Pageable pageable) {
        if (termo == null || termo.trim().length() < 2) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Termo de busca deve ter pelo menos 2 caracteres.");
        }

        Page<Profissional> page = profissionalRepository.findByTermoOrdenados(termo.trim(), pageable);

        List<ProfissionalCardDTO> cards = page.getContent().stream()
                .map(p -> toCardDTO(p, null))
                .toList();

        return new ProfissionalBuscaResponse(
                cards,
                page.getTotalElements(),
                page.getNumber(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<ProfissionalCardDTO> buscarSugeridos(Double lat, Double lng) {
        List<Object[]> rows = profissionalRepository.findSugeridosWithDistance(lat, lng, 10_000.0);

        List<Long> ids = rows.stream()
                .map(r -> ((Number) r[0]).longValue())
                .toList();
        Map<Long, Double> distancias = rows.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r[0]).longValue(),
                        r -> ((Number) r[1]).doubleValue()
                ));

        List<Profissional> profissionais = profissionalRepository.findAllById(ids);
        Map<Long, Profissional> profMap = profissionais.stream()
                .collect(Collectors.toMap(Profissional::getId, p -> p));

        return ids.stream()
                .map(id -> toCardDTO(profMap.get(id), distancias.get(id)))
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfissionalPerfilDTO buscarPerfil(Long profissionalId) {
        Profissional p = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profissional não encontrado."));

        if (Boolean.FALSE.equals(p.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado.");
        }

        List<String> portfolio = p.getPortfolio() != null ? p.getPortfolio() : List.of();
        List<String> portfolioFotos = portfolio.stream().limit(3).toList();

        // força a criação de uma lista ao invés de utilizar um proxy
        List<String> especialidades = p.getEspecialidades() != null
                ? new ArrayList<>(p.getEspecialidades())
                : List.of();

        return new ProfissionalPerfilDTO(
                p.getId(),
                p.getNome(),
                p.getFoto(),
                p.getPlanoPlus(),
                p.getDisponivel(),
                primaryCategoriaNome(p),
                p.getCidade() + "," + p.getEstado(),
                p.getNotaMedia(),
                p.getTotalAvaliacoes(),
                p.getAnosExperiencia(),
                p.getTotalServicosExecutados(),
                p.getDescricaoEspecialidades(),
                especialidades,
                p.getBio(),
                portfolioFotos,
                portfolio.size()
        );
    }

    @Transactional(readOnly = true)
    public List<String> listarPortfolio(Long profissionalId, Pageable pageable) {
        Profissional p = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profissional não encontrado."));

        List<String> portfolio = p.getPortfolio() != null ? p.getPortfolio() : List.of();
        int start = (int) pageable.getOffset();
        if (start >= portfolio.size()) return List.of();
        int end = Math.min(start + pageable.getPageSize(), portfolio.size());
        return new ArrayList<>(portfolio.subList(start, end));
    }

    private ProfissionalCardDTO toCardDTO(Profissional p, Double distanciaKm) {
        if (p == null) return null;
        return new ProfissionalCardDTO(
                p.getId(),
                p.getNome(),
                p.getFoto(),
                p.getPlanoPlus(),
                primaryCategoriaNome(p),
                p.getCidade(),
                distanciaKm,
                p.getNotaMedia(),
                p.getTotalAvaliacoes()
        );
    }

    private String primaryCategoriaNome(Profissional p) {
        List<CategoriaServico> cats = p.getCategoriasAtendidas();
        if (cats == null || cats.isEmpty()) return null;
        CategoriaServico primary = cats.get(0);
        return categoriaNames.getOrDefault(primary, primary.name());
    }
}
