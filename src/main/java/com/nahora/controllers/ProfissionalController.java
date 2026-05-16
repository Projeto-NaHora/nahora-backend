package com.nahora.controllers;

import com.nahora.dto.request.CompletarPerfilRequestDTO;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.services.ProfissionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/profissionais")
@RequiredArgsConstructor
@Tag(name = "Profissionais", description = "Gerenciamento do perfil e status de profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    @PutMapping("/perfil")
    @Operation(summary = "Completa ou atualiza o perfil do profissional autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou portfólio com mais de 10 fotos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: Usuário autenticado não é um profissional"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado no banco de dados")
    })
    public ResponseEntity<PerfilProfissionalResponseDTO> completarPerfil(
            @Valid @RequestBody CompletarPerfilRequestDTO dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        if (!(usuarioAutenticado instanceof Profissional)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito para profissionais.");
        }

        // Extrai o ID direto do token 
        PerfilProfissionalResponseDTO response = profissionalService.completarPerfil(usuarioAutenticado.getId(), dto);

        return ResponseEntity.ok(response);
    }
}