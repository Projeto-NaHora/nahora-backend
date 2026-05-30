package com.nahora.jobs;

import com.nahora.model.Pedido;
import com.nahora.model.enums.StatusPedido;
import com.nahora.repositories.PedidoRepository;
import com.nahora.services.PedidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoConfirmacaoJob {

    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;

    @Scheduled(fixedDelay = 60_000)
    public void executar() {
        LocalDateTime prazo = LocalDateTime.now().minusHours(48);
        List<Pedido> pedidos = pedidoRepository.findByStatusAndConcluidoEmLessThanEqual(
                StatusPedido.AGUARDANDO_VALIDACAO, prazo);

        for (Pedido pedido : pedidos) {
            try {
                pedidoService.confirmarConclusao(pedido.getId(), null, true);
                log.info("Auto-confirmação pedido={} concluidoEm={}", pedido.getId(), pedido.getConcluidoEm());
            } catch (Exception ex) {
                log.error("Falha na auto-confirmação pedido={}", pedido.getId(), ex);
            }
        }
    }
}
