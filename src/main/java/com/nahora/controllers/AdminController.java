package com.nahora.controllers;

import com.nahora.dto.request.AdminVerificarRequest;
import com.nahora.services.ProfissionalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Operações administrativas")
public class AdminController {

    private final ProfissionalService profissionalService;

    @PatchMapping("/profissionais/{id}/verificar")
    @Operation(summary = "A13_1 — Aprova ou rejeita a verificação de identidade do profissional")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Status atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campo 'aprovado' ausente"),
            @ApiResponse(responseCode = "404", description = "Profissional não encontrado")
    })
    public ResponseEntity<Void> verificarProfissional(
            @PathVariable Long id,
            @Valid @RequestBody AdminVerificarRequest request) {

        profissionalService.aprovarProfissional(id, request.aprovado());
        return ResponseEntity.noContent().build();
    }
}
