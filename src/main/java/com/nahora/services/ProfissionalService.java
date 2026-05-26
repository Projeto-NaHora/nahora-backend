package com.nahora.services;

import com.nahora.dto.request.ProfissionalCategoriaRequest;
import com.nahora.dto.request.ProfissionalDocumentosRequest;
import com.nahora.dto.request.ProfissionalPerfilRequest;
import com.nahora.dto.response.AdminProfissionalPendenteDTO;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Profissional;
import com.nahora.model.enums.StatusVerificacao;
import com.nahora.repositories.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    private final PushNotificationService pushNotificationService;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional(readOnly = true)
    public PerfilProfissionalResponseDTO getPerfil(Long profissionalId) {
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado."));
        return toResponseDTO(profissional);
    }

    @Transactional
    public void salvarCategoria(Long profissionalId, ProfissionalCategoriaRequest dto) {
        Profissional profissional = findOrThrow(profissionalId);
        requireStatus(profissional, StatusVerificacao.CADASTRO_INCOMPLETO,
                "Categoria só pode ser definida no início do cadastro.");

        profissional.setCategoriasAtendidas(new java.util.ArrayList<>(java.util.List.of(dto.categoria())));
        profissionalRepository.save(profissional);
    }

    @Transactional
    public void salvarDocumentos(Long profissionalId, ProfissionalDocumentosRequest dto) {
        Profissional profissional = findOrThrow(profissionalId);
        requireStatus(profissional, StatusVerificacao.CADASTRO_INCOMPLETO,
                "Documentos só podem ser enviados no início do cadastro.");

        profissional.setRgFrenteUrl(dto.rgFrente());
        profissional.setRgVersoUrl(dto.rgVerso());
        profissional.setSelfieUrl(dto.selfieComDocumento());
        profissional.setStatusVerificacao(StatusVerificacao.AGUARDANDO_VERIFICACAO);
        profissionalRepository.save(profissional);
    }

    @Transactional
    public PerfilProfissionalResponseDTO atualizarPerfil(Long profissionalId, ProfissionalPerfilRequest dto) {
        Profissional profissional = findOrThrow(profissionalId);
        requireStatus(profissional, StatusVerificacao.VERIFICADO,
                "O perfil só pode ser editado após a verificação de identidade.");

        if (dto.nome() != null)       profissional.setNome(dto.nome());
        if (dto.email() != null)      profissional.setEmail(dto.email());
        if (dto.cpf() != null)        profissional.setCpf(dto.cpf());
        if (dto.celular() != null)    profissional.setTelefone(dto.celular());
        if (dto.fotoPerfil() != null) profissional.setFoto(dto.fotoPerfil());
        if (dto.profissao() != null)  profissional.setProfissao(dto.profissao());
        if (dto.cep() != null)        profissional.setCep(dto.cep());
        if (dto.bio() != null)        profissional.setBio(dto.bio());
        if (dto.especialidades() != null)    profissional.setEspecialidades(dto.especialidades());
        if (dto.categorias() != null)        profissional.setCategoriasAtendidas(dto.categorias());
        if (dto.anosExperiencia() != null)   profissional.setAnosExperiencia(dto.anosExperiencia());
        if (dto.raioAtuacaoKm() != null)     profissional.setRaioAtuacao(dto.raioAtuacaoKm());
        if (dto.urlsFotosPortfolio() != null) profissional.setPortfolio(dto.urlsFotosPortfolio());

        if (dto.latitude() != null || dto.longitude() != null) {
            if (dto.latitude() == null || dto.longitude() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Latitude e longitude devem ser informadas juntas.");
            }
            Point ponto = geometryFactory.createPoint(new Coordinate(dto.longitude(), dto.latitude()));
            profissional.setLocalizacao(ponto);
        }

        profissional.setPerfilCompleto(true);
        profissional = profissionalRepository.save(profissional);
        return toResponseDTO(profissional);
    }

    @Transactional(readOnly = true)
    public Page<AdminProfissionalPendenteDTO> listarPendentes(Pageable pageable) {
        return profissionalRepository
                .findByStatusVerificacao(StatusVerificacao.AGUARDANDO_VERIFICACAO, pageable)
                .map(this::toPendenteDTO);
    }

    @Transactional
    public void aprovarProfissional(Long id, boolean aprovado) {
        Profissional profissional = findOrThrow(id);

        if (aprovado) {
            profissional.setStatusVerificacao(StatusVerificacao.VERIFICADO);
            if (profissional.getTokenFcm() != null) {
                pushNotificationService.enviarNotificacao(
                        profissional.getTokenFcm(),
                        "Identidade verificada!",
                        "Seu perfil foi aprovado. Agora você pode criar seu perfil profissional."
                );
            }
        } else {
            profissional.setStatusVerificacao(StatusVerificacao.REJEITADO);
        }

        profissionalRepository.save(profissional);
    }

    private Profissional findOrThrow(Long id) {
        return profissionalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado."));
    }

    private void requireStatus(Profissional profissional, StatusVerificacao esperado, String mensagem) {
        if (profissional.getStatusVerificacao() != esperado) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, mensagem);
        }
    }

    private AdminProfissionalPendenteDTO toPendenteDTO(Profissional p) {
        return new AdminProfissionalPendenteDTO(
                p.getId(),
                p.getNome(),
                p.getTelefone(),
                p.getEmail(),
                p.getRgFrenteUrl(),
                p.getRgVersoUrl(),
                p.getSelfieUrl(),
                p.getCriadoEm()
        );
    }

    private PerfilProfissionalResponseDTO toResponseDTO(Profissional p) {
        return new PerfilProfissionalResponseDTO(
                p.getId(),
                p.getNome(),
                p.getFoto(),
                p.getProfissao(),
                p.getCep(),
                p.getBio(),
                p.getCategoriasAtendidas() != null ? new java.util.ArrayList<>(p.getCategoriasAtendidas()) : null,
                p.getEspecialidades() != null ? new java.util.ArrayList<>(p.getEspecialidades()) : null,
                p.getAnosExperiencia(),
                p.getRaioAtuacao(),
                p.getNotaMedia(),
                p.getTotalAvaliacoes(),
                p.getTotalServicosExecutados(),
                p.getPortfolio() != null ? new java.util.ArrayList<>(p.getPortfolio()) : null,
                p.getDisponivel(),
                p.getStatusVerificacao()
        );
    }
}
