package com.nahora.services;

import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import com.nahora.repositories.ProfissionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final ProfissionalRepository profissionalRepository;
    private final PushNotificationService pushNotificationService;

    @Async
    public void notificarProfissionaisDaCategoria(Pedido pedido) {
        if (pedido.getEndereco() == null || pedido.getEndereco().getCoordenadas() == null) return;

        double latPedido = pedido.getEndereco().getCoordenadas().getY();
        double lonPedido = pedido.getEndereco().getCoordenadas().getX();
        double raioMaximoKm = 10.0;

        List<Profissional> elegiveis = profissionalRepository
                .findByCategoriasAtendidasAndAtivoTrueAndPerfilCompletoTrue(pedido.getCategoria());

        for (Profissional profissional : elegiveis) {
            if (profissional.getLocalizacao() != null) {
                double distancia = haversineKm(
                        latPedido, lonPedido,
                        profissional.getLocalizacao().getY(),
                        profissional.getLocalizacao().getX()
                );
                if (distancia <= raioMaximoKm) {
                    pushNotificationService.enviarNotificacaoNovoPedido(profissional, pedido);
                }
            }
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
