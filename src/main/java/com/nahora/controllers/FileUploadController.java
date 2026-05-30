package com.nahora.controllers;

import com.nahora.dto.response.UploadResponse;
import com.nahora.services.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Upload", description = "Upload de arquivos para o storage")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Validated
public class FileUploadController {

    private final StorageService storageService;

    @Operation(summary = "Faz upload de um documento do profissional",
               description = "Tipos aceitos: RG_FRENTE, RG_VERSO, SELFIE, PEDIDO, PORTIFOLIO, PERFIL. Retorna a URL pública do arquivo.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tipo")
            @Pattern(regexp = "RG_FRENTE|RG_VERSO|SELFIE|PEDIDO|PORTIFOLIO|PERFIL", message = "tipo deve ser RG_FRENTE, RG_VERSO, SELFIE, PEDIDO, PORTIFOLIO ou PERFIL")
            String tipo
    ) {
        String url = storageService.uploadDocumento(file, tipo);
        return ResponseEntity.ok(new UploadResponse(url));
    }
}
