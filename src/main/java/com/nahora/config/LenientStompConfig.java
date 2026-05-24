package com.nahora.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

@Component
public class LenientStompConfig {

    @Autowired
    private ApplicationContext context;

    @EventListener(ApplicationReadyEvent.class)
    public void configureLenientStomp() {
        SubProtocolWebSocketHandler handler = context.getBean(SubProtocolWebSocketHandler.class);
        for (SubProtocolHandler subProtocol : handler.getProtocolHandlers()) {
            if (subProtocol instanceof StompSubProtocolHandler stomp) {
                stomp.setDecoder(new LenientStompDecoder());
            }
        }
    }
}
