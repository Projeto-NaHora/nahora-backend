package com.nahora.controllers;

import com.nahora.dto.request.ProfissionalCategoriaRequest;
import com.nahora.dto.request.ProfissionalDocumentosRequest;
import com.nahora.dto.request.ProfissionalPerfilRequest;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Profissional;
import com.nahora.model.Usuario;
import com.nahora.services.ProfissionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Profissionais", description = "Gerenciamento do perfil e status de profissionais")
public class ProfissionalController {

    private final ProfissionalService profissionalService;

    @PostMapping("/categoria")
    @Operation(summary = "A11 — Seleciona a categoria de serviço do profissional")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria salva com sucesso"),
            @ApiResponse(responseCode = "400", description = "Categoria inválida"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: não é profissional ou status inválido"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<Void> selecionarCategoria(
            @Valid @RequestBody ProfissionalCategoriaRequest dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        profissionalService.salvarCategoria(profissional(usuarioAutenticado).getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/documentos")
    @Operation(summary = "A12 — Envia documentos de verificação de identidade")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Documentos enviados, status atualizado para AGUARDANDO_VERIFICACAO"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: não é profissional ou status inválido"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<Void> enviarDocumentos(
            @Valid @RequestBody ProfissionalDocumentosRequest dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        profissionalService.salvarDocumentos(profissional(usuarioAutenticado).getId(), dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/perfil")
    @Operation(summary = "Retorna os dados atuais do perfil para pré-preenchimento")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: não é profissional"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<PerfilProfissionalResponseDTO> getPerfil(
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        PerfilProfissionalResponseDTO response = profissionalService.getPerfil(profissional(usuarioAutenticado).getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/perfil")
    @Operation(summary = "A14–A16 — Cadastra ou edita o perfil do profissional verificado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos inválidos ou portfólio com mais de 10 fotos"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: não é profissional ou ainda não verificado"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<PerfilProfissionalResponseDTO> atualizarPerfil(
            @Valid @RequestBody ProfissionalPerfilRequest dto,
            @AuthenticationPrincipal Usuario usuarioAutenticado) {

        PerfilProfissionalResponseDTO response = profissionalService.atualizarPerfil(profissional(usuarioAutenticado).getId(), dto);
        return ResponseEntity.ok(response);
    }

    private Profissional profissional(Usuario usuario) {
        if (!(usuario instanceof Profissional p)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito para profissionais.");
        }
        return p;
    }
}
