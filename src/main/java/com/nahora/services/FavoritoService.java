package com.nahora.services;

import com.nahora.dto.response.FavoritoResponseDTO;
import com.nahora.model.Categoria;
import com.nahora.model.Cliente;
import com.nahora.model.Favorito;
import com.nahora.model.Profissional;
import com.nahora.model.enums.CategoriaServico;
import com.nahora.repositories.CategoriaRepository;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.FavoritoRepository;
import com.nahora.repositories.ProfissionalRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritoService {

    private final FavoritoRepository favoritoRepository;
    private final ProfissionalRepository profissionalRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;

    private Map<CategoriaServico, String> categoriaNames = new HashMap<>();

    @PostConstruct
    void initCategoriaCache() {
        categoriaNames = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(Categoria::getCategoriaServico, Categoria::getNome));
    }

    @Transactional
    public void adicionarFavorito(Long clienteId, Long profissionalId) {
        if (!profissionalRepository.existsById(profissionalId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado.");
        }
        if (favoritoRepository.existsByClienteIdAndProfissionalId(clienteId, profissionalId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profissional já está nos favoritos.");
        }

        Cliente cliente = clienteRepository.getReferenceById(clienteId);
        Profissional profissional = profissionalRepository.getReferenceById(profissionalId);

        Favorito favorito = new Favorito();
        favorito.setCliente(cliente);
        favorito.setProfissional(profissional);
        favoritoRepository.save(favorito);
    }

    @Transactional
    public void removerFavorito(Long clienteId, Long profissionalId) {
        Favorito favorito = favoritoRepository.findByClienteIdAndProfissionalId(clienteId, profissionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Favorito não encontrado."));
        favoritoRepository.delete(favorito);
    }

    @Transactional(readOnly = true)
    public Page<FavoritoResponseDTO> listarFavoritos(Long clienteId, Long categoriaId, Pageable pageable) {
        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "criadoEm"));

        Page<Favorito> page;
        if (categoriaId != null) {
            Categoria categoria = categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));
            page = favoritoRepository.findByClienteIdAndCategoria(clienteId, categoria.getCategoriaServico(), sorted);
        } else {
            page = favoritoRepository.findByClienteId(clienteId, sorted);
        }

        return page.map(this::toResponseDTO);
    }

    public boolean isFavoritado(Long clienteId, Long profissionalId) {
        return favoritoRepository.existsByClienteIdAndProfissionalId(clienteId, profissionalId);
    }

    private FavoritoResponseDTO toResponseDTO(Favorito f) {
        Profissional p = f.getProfissional();
        List<String> categorias = p.getCategoriasAtendidas() != null
                ? p.getCategoriasAtendidas().stream()
                        .map(c -> categoriaNames.getOrDefault(c, c.name()))
                        .toList()
                : List.of();
        return new FavoritoResponseDTO(
                p.getId(),
                p.getNome(),
                p.getFoto(),
                p.getNotaMedia(),
                p.getTotalAvaliacoes(),
                categorias,
                f.getCriadoEm());
    }
}
