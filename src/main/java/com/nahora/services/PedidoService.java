package com.nahora.services;

import com.nahora.dto.request.PedidoRequest;
import com.nahora.dto.response.EnderecoResponse;
import com.nahora.dto.response.PedidoResponse;
import com.nahora.model.Cliente;
import com.nahora.model.Endereco;
import com.nahora.model.Pedido;
import com.nahora.model.enums.StatusPedido;
import com.nahora.repositories.ClienteRepository;
import com.nahora.repositories.PedidoRepository;
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

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

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
}
