package com.nahora.services;

import com.nahora.dto.request.PedidoDistanceRequest;
import com.nahora.dto.request.PedidoFiltroRequest;
import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.AceitarPropostaResponseDTO;
import com.nahora.dto.response.EnderecoResponse;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.dto.response.PedidoResumoResponse;
import com.nahora.dto.response.PropostaResponseDTO;
import com.nahora.model.Cliente;
import com.nahora.model.Endereco;
import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import com.nahora.model.Proposta;
import com.nahora.model.Usuario;
import com.nahora.model.enums.StatusPedido;
import com.nahora.model.enums.StatusProposta;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.PedidoRepository;
import com.nahora.repositories.ProfissionalRepository;
import com.nahora.repositories.PropostaRepository;
import com.nahora.repositories.specification.PedidoSpecifications;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ProfissionalRepository profissionalRepository;

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
        pedido.setDescricao(request.getDescricao().trim());
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
        response.setFotos(List.copyOf(pedido.getFotos()));
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
        if (pedido.getProfissionalAtribuido() != null) {
            response.setProfissionalAtribuidoId(pedido.getProfissionalAtribuido().getId());
            response.setProfissionalAtribuidoNome(pedido.getProfissionalAtribuido().getNome());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPedidoPorId(Long pedidoId, Usuario usuario) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));

        boolean isCliente = usuario instanceof Cliente && pedido.getCliente().getId().equals(usuario.getId());
        boolean isProfissionalAtribuido = usuario instanceof Profissional
                && pedido.getProfissionalAtribuido() != null
                && pedido.getProfissionalAtribuido().getId().equals(usuario.getId());

        if (!isCliente && !isProfissionalAtribuido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado ao pedido");
        }

        return toResponseDTO(pedido);
    }

    private Profissional getProfissionalAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        if (principal instanceof Profissional profissional) {
            return profissional;
        }

        if (principal instanceof Usuario) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Apenas profissionais podem acessar este recurso");
        }

        String email = authentication.getName();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        return profissionalRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Profissional não encontrado"));
    }

    public Page<PedidoResumoResponse> listarPedidosComFiltros(
            PedidoFiltroRequest filtro,
            Pageable pageable) {

        Profissional profissional = getProfissionalAutenticado();
        Point localizacao = profissional.getLocalizacao();
        Double raioAtuacao = profissional.getRaioAtuacao();
        boolean possuiLocalizacao = localizacao != null && raioAtuacao != null;

        Specification<Pedido> spec = Specification
                .where(PedidoSpecifications.isAberto())
                .and(PedidoSpecifications.hasCategoria(filtro.getCategoria()))
                .and(PedidoSpecifications.isUrgente(filtro.getUrgente()));

        PedidoFiltroRequest.SortBy sortBy = filtro.getSortBy();
        if (sortBy == null) {
            sortBy = PedidoFiltroRequest.SortBy.MAIS_RECENTES;
        }
        if (!possuiLocalizacao && sortBy == PedidoFiltroRequest.SortBy.MAIS_PROXIMOS) {
            sortBy = PedidoFiltroRequest.SortBy.MAIS_RECENTES;
        }

        Sort sort;
        switch (sortBy) {
            case MAIS_RECENTES -> sort = Sort.by(Sort.Direction.DESC, "criadoEm");
            case MAIS_PROXIMOS -> sort = Sort.by(Sort.Direction.ASC, "distanciaKm");
            case URGENTES -> sort = Sort.by(Sort.Direction.DESC, "urgente")
                    .and(Sort.by(Sort.Direction.DESC, "criadoEm"));
            default -> sort = Sort.by(Sort.Direction.DESC, "criadoEm");
        }

        Pageable pageableComSort = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );

        if (!possuiLocalizacao) {
            Page<Pedido> pedidoPage = pedidoRepository.findAll(spec, pageableComSort);
            return pedidoPage.map(pedido -> {
                int numPropostas = propostaRepository.countByPedidoId(pedido.getId());
                return PedidoResumoResponse.fromPedido(pedido, null, numPropostas);
            });
        }

        Page<PedidoDistanceRequest> dtoPage = pedidoRepository.findWithFiltersAndDistance(
                localizacao, raioAtuacao, spec, pageableComSort
        );

        return dtoPage.map(dto -> {
            int numPropostas = propostaRepository.countByPedidoId(dto.getPedido().getId());
            return PedidoResumoResponse.fromPedido(dto.getPedido(), dto.getDistanciaKm(), numPropostas);
        });
    } // <--- CHAVE ADICIONADA AQUI

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
        List<Proposta> propostas = propostaRepository.findByPedidoIdAndStatus(pedidoId, StatusProposta.ATIVA);

        // Algoritmos de ordenação baseados no critério de aceite
        if ("avaliacao".equalsIgnoreCase(ordenarPor)) {
            propostas.sort((p1, p2) -> p2.getProfissional().getNotaMedia().compareTo(p1.getProfissional().getNotaMedia()));
        } else if ("preco".equalsIgnoreCase(ordenarPor)) {
            propostas.sort((p1, p2) -> p1.getValorOferecido().compareTo(p2.getValorOferecido()));
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
            if (!p.getId().equals(propostaId) && p.getStatus() == StatusProposta.ATIVA) {
                p.setStatus(StatusProposta.RECUSADA);

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

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarMeusPedidos(Long clienteId, List<StatusPedido> status, Pageable pageable) {
        Page<Pedido> pedidos = status != null && !status.isEmpty()
                ? pedidoRepository.findByClienteIdAndStatusIn(clienteId, status, pageable)
                : pedidoRepository.findByClienteId(clienteId, pageable);
        return pedidos.map(this::toResponseDTO);
    }

    private PropostaResponseDTO mapToPropostaResponseDTO(Proposta proposta) {
        var prof = proposta.getProfissional();

        List<PropostaResponseDTO.JanelaHorarioDTO> horarios = proposta.getHorarios().stream()
                .map(h -> new PropostaResponseDTO.JanelaHorarioDTO(h.getDataHoraInicio(), h.getDataHoraFim()))
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
                proposta.getValorOferecido(),
                horarios,
                proposta.getStatus()
        );
    }
}