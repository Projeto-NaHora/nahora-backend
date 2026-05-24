package com.nahora.services;

import com.nahora.model.Pedido;
import com.nahora.model.Profissional;
import com.nahora.services.LogPushNotificationService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class LogPushNotificationServiceTest {

    @InjectMocks
    private LogPushNotificationService logPushNotificationService;

    @Test
    void enviarNotificacaoNovoPedido_DeveExecutarComSucessoSemLancarExcecoes() {
        Profissional profissional = new Profissional();
        profissional.setId(10L);
        profissional.setNome("Carlos Silva");

        Pedido pedido = new Pedido();
        pedido.setId(100L);

        assertThatCode(() -> logPushNotificationService.enviarNotificacaoNovoPedido(profissional, pedido))
                .doesNotThrowAnyException();
    }
}