package com.nahora.services;

import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.AceitarPropostaResponseDTO;
import com.nahora.dto.response.EnderecoResponse;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PropostaResponseDTO;
import com.nahora.model.Cliente;
import com.nahora.model.Endereco;
import com.nahora.model.Pedido;
import com.nahora.model.Proposta;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final PropostaRepository propostaRepository;

    private static final Set<StatusPedido> STATUS_EM_ABERTO = Set.of(
            StatusPedido.ABERTO,
            StatusPedido.EM_ANDAMENTO,
            StatusPedido.AGUARDANDO_VALIDACAO,
            StatusPedido.EM_DISPUTA
    );

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public Pedido criarPedido(Long clienteId, PedidoRequest request) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cliente não encontrado com ID: " + clienteId));

        long pedidosAbertos = pedidoRepository.countByClienteAndStatusIn(cliente, STATUS_EM_ABERTO);
        if (pedidosAbertos >= 3) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Cliente já possui %d pedidos em aberto. Limite máximo é 3.", pedidosAbertos));
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setCategoria(request.getCategoria());
        pedido.setDescricao(request.getDescricao());
        pedido.setUrgencia(request.getUrgencia());
        pedido.setOrcamentoEstimado(request.getOrcamentoEstimado());
        pedido.setDataDesejada(request.getDataDesejada());
        pedido.setStatus(StatusPedido.ABERTO);
        pedido.setFotos(request.getFotos() != null ? request.getFotos() : List.of());

        Endereco endereco;
        if (request.getEnderecoSalvoIndex() != null) {
            List<Endereco> enderecosSalvos = cliente.getEnderecosSalvos();
            int index = request.getEnderecoSalvoIndex();
            if (index < 0 || index >= enderecosSalvos.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Índice de endereço salvo inválido. Cliente tem " + enderecosSalvos.size() + " endereço(s).");
            }
            Endereco original = enderecosSalvos.get(index);
            endereco = new Endereco();
            endereco.setLogradouro(original.getLogradouro());
            endereco.setNumero(original.getNumero());
            endereco.setComplemento(original.getComplemento());
            endereco.setBairro(original.getBairro());
            endereco.setCidade(original.getCidade());
            endereco.setEstado(original.getEstado());
            endereco.setCep(original.getCep());
            if (original.getCoordenadas() != null) {
                Point point = geometryFactory.createPoint(original.getCoordenadas().getCoordinate());
                point.setSRID(4326);
                endereco.setCoordenadas(point);
            }
        } else {
            if (request.getEndereco() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "É necessário informar o endereço (novo ou índice de endereço salvo).");
            }
            endereco = new Endereco();
            endereco.setLogradouro(request.getEndereco().getLogradouro());
            endereco.setNumero(request.getEndereco().getNumero());
            endereco.setComplemento(request.getEndereco().getComplemento());
            endereco.setBairro(request.getEndereco().getBairro());
            endereco.setCidade(request.getEndereco().getCidade());
            endereco.setEstado(request.getEndereco().getEstado());
            endereco.setCep(request.getEndereco().getCep());

            Double lat = request.getEndereco().getLatitude();
            Double lng = request.getEndereco().getLongitude();
            if (lat != null && lng != null) {
                Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
                point.setSRID(4326);
                endereco.setCoordenadas(point);
            }
        }
        pedido.setEndereco(endereco);

        return pedidoRepository.save(pedido);
    }

    public PedidoResponse toResponseDTO(Pedido pedido) {
        PedidoResponse response = new PedidoResponse();
        response.setId(pedido.getId());
        response.setCategoria(pedido.getCategoria());
        response.setDescricao(pedido.getDescricao());
        response.setFotos(pedido.getFotos());
        response.setUrgencia(pedido.getUrgencia());
        response.setOrcamentoEstimado(pedido.getOrcamentoEstimado());
        response.setDataDesejada(pedido.getDataDesejada());
        response.setStatus(pedido.getStatus());
        response.setCriadoEm(pedido.getCriadoEm());
        response.setClienteId(pedido.getCliente().getId());
        response.setClienteNome(pedido.getCliente().getNome());

        if (pedido.getEndereco() != null) {
            EnderecoResponse enderecoResponse = new EnderecoResponse();
            enderecoResponse.setLogradouro(pedido.getEndereco().getLogradouro());
            enderecoResponse.setNumero(pedido.getEndereco().getNumero());
            enderecoResponse.setComplemento(pedido.getEndereco().getComplemento());
            enderecoResponse.setBairro(pedido.getEndereco().getBairro());
            enderecoResponse.setCidade(pedido.getEndereco().getCidade());
            enderecoResponse.setEstado(pedido.getEndereco().getEstado());
            enderecoResponse.setCep(pedido.getEndereco().getCep());
            if (pedido.getEndereco().getCoordenadas() != null) {
                enderecoResponse.setLongitude(pedido.getEndereco().getCoordenadas().getX());
                enderecoResponse.setLatitude(pedido.getEndereco().getCoordenadas().getY());
            }
            response.setEndereco(enderecoResponse);
        }
        return response;
    }
    /**
     * UC-09: Lista as propostas PENDENTES (Ativas no modelo atual) vinculadas a um pedido.
     */
    @Transactional(readOnly = true)
    public List<PropostaResponseDTO> listarPropostasAtivas(Long pedidoId, Long clienteId, String ordenarPor) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        // Regra de segurança: O usuário logado precisa ser o criador do pedido
        if (!pedido.getCliente().getId().equals(clienteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário não autorizado a visualizar estas propostas");
        }

        // Mapeia conceito de Proposta ATIVA para o seu Enum PENDENTE
        List<Proposta> propostas = propostaRepository.findByPedidoIdAndStatus(pedidoId, StatusProposta.PENDENTE);

        // Algoritmos de ordenação baseados no critério de aceite
        if ("avaliacao".equalsIgnoreCase(ordenarPor)) {
            propostas.sort((p1, p2) -> p2.getProfissional().getNotaMedia().compareTo(p1.getProfissional().getNotaMedia()));
        } else if ("preco".equalsIgnoreCase(ordenarPor)) {
            propostas.sort((p1, p2) -> p1.getValor().compareTo(p2.getValor()));
        }

        return propostas.stream().map(this::mapToPropostaResponseDTO).collect(Collectors.toList());
    }

    @Transactional
    public AceitarPropostaResponseDTO aceitarProposta(Long pedidoId, Long propostaId, Long clienteId) {
        // Validar se o pedido existe
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        // Validar se o usuário autenticado é o criador do pedido
        if (!pedido.getCliente().getId().equals(clienteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuário autenticado não é o dono do pedido");
        }

        // Validar se o pedido está com status ABERTO
        if (pedido.getStatus() != StatusPedido.ABERTO) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Pedido não está com status ABERTO");
        }

        // Validar se a proposta existe
        Proposta propostaEscolhida = propostaRepository.findById(propostaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proposta não encontrada"));

        // Validar se a proposta pertence ao pedido informado
        if (!propostaEscolhida.getPedido().getId().equals(pedidoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "A proposta informada não pertence a este pedido");
        }


        // Mudar o status da proposta selecionada para ACEITA
        propostaEscolhida.setStatus(StatusProposta.ACEITA);

        // Mudar o status das outras propostas PENDENTES vinculadas ao pedido para REJEITADA automaticamente
        List<Proposta> todasPropostas = propostaRepository.findByPedidoId(pedidoId);
        for (Proposta p : todasPropostas) {
            if (!p.getId().equals(propostaId) && p.getStatus() == StatusProposta.PENDENTE) {
                p.setStatus(StatusProposta.REJEITADA);

                // Mock do encerramento de canais de Chat (UH-27 / Módulo de Mensageria)
                System.out.println("[CHAT MOCK] Encerrando canal de chat do pedido " + pedidoId + " para o profissional " + p.getProfissional().getId());
            }
        }

        // Atualizar o status do Pedido para EM_ANDAMENTO e atribuir o profissional
        pedido.setStatus(StatusPedido.EM_ANDAMENTO);
        pedido.setProfissionalAtribuido(propostaEscolhida.getProfissional());
        pedidoRepository.save(pedido);

        // Mock do NotificationService para disparar notificação push ao profissional aceito
        System.out.println("[NOTIFICATION MOCK] Enviando Push para o Profissional " + propostaEscolhida.getProfissional().getId() + ": Sua proposta foi aceita!");

        return new AceitarPropostaResponseDTO(
                pedido.getId(),
                pedido.getStatus(),
                propostaEscolhida.getProfissional().getId(),
                propostaEscolhida.getProfissional().getNome()
        );
    }

    private PropostaResponseDTO mapToPropostaResponseDTO(Proposta proposta) {
        var prof = proposta.getProfissional();

        List<PropostaResponseDTO.JanelaHorarioDTO> horarios = proposta.getHorariosDisponiveis().stream()
                .map(h -> new PropostaResponseDTO.JanelaHorarioDTO(h.getInicio(), h.getFim()))
                .collect(Collectors.toList());

        return new PropostaResponseDTO(
                proposta.getId(),
                prof.getNome(),
                prof.getFoto(),
                prof.getNotaMedia(),
                prof.getNumeroAvaliacoes(),
                prof.getTotalServicosExecutados(),
                0.0, // Distância simulada
                proposta.getDescricao(),
                proposta.getValor(),
                horarios,
                proposta.getStatus()
        );
    }
}
