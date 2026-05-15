package com.nahora.services;

import com.nahora.dto.request.HorarioPropostaDTO;
import com.nahora.dto.request.PropostaRequestDTO;
import com.nahora.dto.response.HorarioPropostaResponse;
import com.nahora.dto.response.PropostaResponse;
import com.nahora.model.HorarioProposta;
import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import com.nahora.model.Proposta;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.ProfissionalRepository;
import com.nahora.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PropostaService {

    private final PropostaRepository propostaRepository;
    private final PedidoRepository pedidoRepository;
    private final ProfissionalRepository profissionalRepository;

    public record PropostaSalvaResult(Proposta proposta, boolean criada) {}

    @Transactional
    public PropostaSalvaResult salvarOuAtualizarProposta(Long pedidoId, Long profissionalId, PropostaRequestDTO dto) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não é possível enviar proposta: o pedido não está com status ABERTO");
        }

        if (pedido.getCliente().getId().equals(profissionalId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Profissional não pode enviar proposta para um pedido que ele mesmo criou");
        }

        Profissional profissional = profissionalRepository.findById(profissionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional não encontrado"));

        validateHorarios(dto.getHorariosDisponiveis());

        Optional<Proposta> propostaExistente = propostaRepository
                .findByPedidoIdAndProfissionalIdAndStatus(pedidoId, profissionalId, StatusProposta.ATIVA);

        if (propostaExistente.isPresent()) {
            if (propostaRepository.existsByPedidoIdAndStatus(pedidoId, StatusProposta.ACEITA)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Não é possível editar a proposta: o cliente já aceitou uma proposta para este pedido");
            }
            return new PropostaSalvaResult(atualizar(propostaExistente.get(), dto), false);
        }

        return new PropostaSalvaResult(criar(pedido, profissional, dto), true);
    }

    private Proposta criar(Pedido pedido, Profissional profissional, PropostaRequestDTO dto) {
        Proposta proposta = new Proposta();
        proposta.setPedido(pedido);
        proposta.setProfissional(profissional);
        proposta.setValorOferecido(dto.getValorOferecido());
        proposta.setDescricao(dto.getDescricao());
        proposta.setStatus(StatusProposta.ATIVA);
        proposta.setHorarios(toHorarioEntities(dto.getHorariosDisponiveis(), proposta));

        Proposta salva = propostaRepository.save(proposta);

        // TODO (UH-27 — ChatService): abrir automaticamente o canal entre cliente e profissional
        // chatService.abrirCanal(pedido.getId(), salva.getId());

        return salva;
    }

    private Proposta atualizar(Proposta proposta, PropostaRequestDTO dto) {
        proposta.setValorOferecido(dto.getValorOferecido());
        proposta.setDescricao(dto.getDescricao());
        proposta.getHorarios().clear();
        proposta.getHorarios().addAll(toHorarioEntities(dto.getHorariosDisponiveis(), proposta));
        return propostaRepository.save(proposta);
    }

    private List<HorarioProposta> toHorarioEntities(List<HorarioPropostaDTO> dtos, Proposta proposta) {
        return dtos.stream().map(dto -> {
            HorarioProposta h = new HorarioProposta();
            h.setProposta(proposta);
            h.setDataHoraInicio(dto.getInicio());
            h.setDataHoraFim(dto.getFim());
            return h;
        }).toList();
    }

    private void validateHorarios(List<HorarioPropostaDTO> horarios) {
        for (HorarioPropostaDTO h : horarios) {
            if (!h.getFim().isAfter(h.getInicio())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O horário de fim deve ser posterior ao horário de início");
            }
        }

        for (int i = 0; i < horarios.size(); i++) {
            for (int j = i + 1; j < horarios.size(); j++) {
                if (seOverpoem(horarios.get(i), horarios.get(j))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Os horários disponíveis não podem se sobrepor");
                }
            }
        }
    }

    private boolean seOverpoem(HorarioPropostaDTO a, HorarioPropostaDTO b) {
        return a.getInicio().isBefore(b.getFim()) && b.getInicio().isBefore(a.getFim());
    }

    public PropostaResponse toResponseDTO(Proposta proposta) {
        PropostaResponse response = new PropostaResponse();
        response.setId(proposta.getId());
        response.setPedidoId(proposta.getPedido().getId());
        response.setProfissionalId(proposta.getProfissional().getId());
        response.setValorOferecido(proposta.getValorOferecido());
        response.setDescricao(proposta.getDescricao());
        response.setStatus(proposta.getStatus());
        response.setCriadoEm(proposta.getCriadoEm());
        response.setHorariosDisponiveis(proposta.getHorarios().stream()
                .map(h -> {
                    HorarioPropostaResponse hr = new HorarioPropostaResponse();
                    hr.setId(h.getId());
                    hr.setInicio(h.getDataHoraInicio());
                    hr.setFim(h.getDataHoraFim());
                    return hr;
                }).toList());
        return response;
    }
}
