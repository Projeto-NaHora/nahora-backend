package com.nahora.controllers;

import com.nahora.dto.response.FavoritoResponseDTO;
import com.nahora.model.Cliente;
import com.nahora.model.Usuario;
import com.nahora.services.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Favoritos", description = "Gerenciar profissionais favoritos do cliente (UC-18)")
public class FavoritoController {

    private final FavoritoService favoritoService;

    @PostMapping("/api/v1/favoritos/{profissionalId}")
    @Operation(summary = "Adiciona profissional aos favoritos do cliente logado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Favorito adicionado"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado"),
            @ApiResponse(responseCode = "409", description = "Profissional já está nos favoritos")
    })
    public ResponseEntity<Void> adicionarFavorito(
            @PathVariable Long profissionalId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        favoritoService.adicionarFavorito(cliente(usuarioAutenticado).getId(), profissionalId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/v1/favoritos/{profissionalId}")
    @Operation(summary = "Remove profissional dos favoritos do cliente logado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Favorito removido"),
            @ApiResponse(responseCode = "404", description = "Favorito não encontrado")
    })
    public ResponseEntity<Void> removerFavorito(
            @PathVariable Long profissionalId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        favoritoService.removerFavorito(cliente(usuarioAutenticado).getId(), profissionalId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/favoritos")
    @Operation(summary = "Lista favoritos do cliente logado (paginado, filtro opcional por categoriaId)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de favoritos retornada"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    })
    public ResponseEntity<Page<FavoritoResponseDTO>> listarFavoritos(
            @RequestParam(required = false) Long categoriaId,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        Page<FavoritoResponseDTO> resultado = favoritoService.listarFavoritos(
                cliente(usuarioAutenticado).getId(), categoriaId, pageable);
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/api/v1/profissionais/{profissionalId}/favoritado")
    @Operation(summary = "Verifica se o profissional está nos favoritos do cliente logado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status de favorito retornado")
    })
    public ResponseEntity<Map<String, Boolean>> isFavoritado(
            @PathVariable Long profissionalId,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {
        boolean favoritado = favoritoService.isFavoritado(
                cliente(usuarioAutenticado).getId(), profissionalId);
        return ResponseEntity.ok(Map.of("favoritado", favoritado));
    }

    private Cliente cliente(Usuario usuario) {
        if (!(usuario instanceof Cliente c)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito para clientes.");
        }
        return c;
    }
}
