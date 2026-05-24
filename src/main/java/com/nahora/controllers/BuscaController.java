package com.nahora.controllers;

import com.nahora.dto.response.*;
import com.nahora.services.BuscaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Busca", description = "Pesquisa filtrada por área profissional (UC-16)")
public class BuscaController {

    private final BuscaService buscaService;

    @GetMapping("/api/v1/profissionais")
    @Operation(summary = "Lista profissionais por categoria (paginado)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profissionais encontrados na categoria"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada")
    })
    public ResponseEntity<ProfissionalBuscaResponse> listarPorCategoria(
            @RequestParam Long categoriaId,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(buscaService.buscarPorCategoria(categoriaId, pageable));
    }

    @GetMapping("/api/v1/profissionais/busca")
    @Operation(summary = "Busca profissionais por nome ou categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado da busca"),
            @ApiResponse(responseCode = "400", description = "Termo muito curto (mínimo 2 caracteres)"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<ProfissionalBuscaResponse> buscarPorTermo(
            @RequestParam String termo,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(buscaService.buscarPorTermo(termo, pageable));
    }

    @GetMapping("/api/v1/profissionais/sugeridos")
    @Operation(summary = "Lista até 10 profissionais próximos ao cliente (raio 10 km)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profissionais sugeridos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<List<ProfissionalCardDTO>> buscarSugeridos(
            @RequestParam Double lat,
            @RequestParam Double lng) {
        return ResponseEntity.ok(buscaService.buscarSugeridos(lat, lng));
    }

    @GetMapping("/api/v1/profissionais/{id}")
    @Operation(summary = "Retorna o perfil completo de um profissional")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil do profissional"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado ou inativo")
    })
    public ResponseEntity<ProfissionalPerfilDTO> buscarPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(buscaService.buscarPerfil(id));
    }

    @GetMapping("/api/v1/profissionais/{id}/portfolio")
    @Operation(summary = "Lista fotos adicionais do portfólio de um profissional (paginado)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fotos do portfólio"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<List<String>> listarPortfolio(
            @PathVariable Long id,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(buscaService.listarPortfolio(id, pageable));
    }
}
