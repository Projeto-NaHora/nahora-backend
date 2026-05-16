package com.nahora.services;

import com.nahora.dto.request.CompletarPerfilRequestDTO;
import com.nahora.dto.response.PerfilProfissionalResponseDTO;
import com.nahora.model.Profissional;
import com.nahora.repositories.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProfissionalService {

    private final ProfissionalRepository profissionalRepository;
    
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public PerfilProfissionalResponseDTO completarPerfil(Long profissionalId, CompletarPerfilRequestDTO dto) {
        
        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado."));

        // Atualizando os campos
        profissional.setBio(dto.bio());
        profissional.setCategoriasAtendidas(dto.categorias());
        profissional.setEspecialidades(dto.especialidades());
        profissional.setAnosExperiencia(dto.anosExperiencia());
        profissional.setRaioAtuacao(dto.raioAtuacaoKm());
        profissional.setPortfolio(dto.urlsFotos());

      
        Point localizacao = geometryFactory.createPoint(new Coordinate(dto.longitude(), dto.latitude()));
        profissional.setLocalizacao(localizacao);

        profissional.setPerfilCompleto(true);

        profissional = profissionalRepository.save(profissional);

        return MapToResponseDTO(profissional);
    }

    private PerfilProfissionalResponseDTO MapToResponseDTO(Profissional p) {
        return new PerfilProfissionalResponseDTO(
                p.getId(),
                p.getNome(),
                p.getFoto(),
                p.getBio(),
                p.getCategoriasAtendidas(),
                p.getEspecialidades(),
                p.getAnosExperiencia(),
                p.getRaioAtuacao(),
                p.getNotaMedia(),
                p.getTotalAvaliacoes(),
                p.getTotalServicosExecutados(),
                p.getPortfolio(),
                p.getDisponivel(),
                p.getStatusVerificacao()
        );
    }
}