package com.nahora.jobs;

import com.nahora.model.Pedido;
import com.nahora.model.enums.StatusPedido;
import com.nahora.repositories.PedidoRepository;
import com.nahora.services.PedidoService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AutoConfirmacaoJobTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private AutoConfirmacaoJob job;

    @Test
    void executar_DeveAutoConfirmarApenasOrdensMaisAntigaQue48h() {
        Pedido pedidoVencido = new Pedido();
        pedidoVencido.setId(1L);
        pedidoVencido.setStatus(StatusPedido.AGUARDANDO_VALIDACAO);
        pedidoVencido.setConcluidoEm(LocalDateTime.now().minusHours(50));

        when(pedidoRepository.findByStatusAndConcluidoEmLessThanEqual(
                eq(StatusPedido.AGUARDANDO_VALIDACAO), any(LocalDateTime.class)))
                .thenReturn(List.of(pedidoVencido));

        job.executar();

        verify(pedidoService).confirmarConclusao(1L, null, true);
    }

    @Test
    void executar_SemPedidosVencidos_NaoDeveChamarConfirmarConclusao() {
        when(pedidoRepository.findByStatusAndConcluidoEmLessThanEqual(any(), any()))
                .thenReturn(List.of());

        job.executar();

        verify(pedidoService, never()).confirmarConclusao(any(), any(), anyBoolean());
    }

    @Test
    void executar_QuandoUmPedidoFalha_DeveContinuarProcessandoOsRestantes() {
        Pedido pedido1 = new Pedido();
        pedido1.setId(1L);
        pedido1.setConcluidoEm(LocalDateTime.now().minusHours(50));

        Pedido pedido2 = new Pedido();
        pedido2.setId(2L);
        pedido2.setConcluidoEm(LocalDateTime.now().minusHours(51));

        when(pedidoRepository.findByStatusAndConcluidoEmLessThanEqual(any(), any()))
                .thenReturn(List.of(pedido1, pedido2));

        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "já concluído"))
                .when(pedidoService).confirmarConclusao(eq(1L), any(), anyBoolean());

        job.executar();

        verify(pedidoService).confirmarConclusao(1L, null, true);
        verify(pedidoService).confirmarConclusao(2L, null, true);
    }

    @Test
    void executar_NaoDeveReprocessarPedidoJaConcluido() {
        when(pedidoRepository.findByStatusAndConcluidoEmLessThanEqual(
                eq(StatusPedido.AGUARDANDO_VALIDACAO), any()))
                .thenReturn(List.of());

        job.executar();

        verify(pedidoService, never()).confirmarConclusao(any(), any(), anyBoolean());
    }
}
