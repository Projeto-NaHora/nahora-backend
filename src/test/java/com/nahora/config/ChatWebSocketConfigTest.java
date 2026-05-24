package com.nahora.config;

import com.nahora.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ChatWebSocketConfigTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private MessageChannel messageChannel;

    private ChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        ChatWebSocketConfig config = new ChatWebSocketConfig(jwtService, userDetailsService);

        // Capture the interceptor registered by the config
        List<ChannelInterceptor> interceptors = new ArrayList<>();
        ChannelRegistration registration = new ChannelRegistration() {
            @Override
            public ChannelRegistration interceptors(ChannelInterceptor... items) {
                interceptors.addAll(List.of(items));
                return this;
            }
        };

        config.configureClientInboundChannel(registration);
        assertEquals(1, interceptors.size(), "Expected exactly one interceptor to be registered");
        interceptor = interceptors.get(0);
    }

    private Message<byte[]> createConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("test-session");
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("Should allow CONNECT without Authorization header")
    void shouldAllowConnectWithoutAuth() {
        Message<byte[]> message = createConnectMessage(null);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result, "Message should not be null (connection allowed)");
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNull(accessor.getUser(), "Session should not be authenticated");
    }

    @Test
    @DisplayName("Should allow CONNECT with invalid JWT — validateToken failed")
    void shouldAllowConnectWithInvalidJwt() {
        String badToken = "Bearer invalid.jwt.token";
        when(jwtService.validateToken("invalid.jwt.token")).thenReturn(false);

        Message<byte[]> message = createConnectMessage(badToken);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result, "Message should not be null (connection allowed despite bad token)");
        verify(jwtService, never()).extractEmail(anyString());
    }

    @Test
    @DisplayName("Should allow CONNECT with valid JWT but user not found")
    void shouldAllowConnectWhenUserNotFound() {
        String validToken = "valid.jwt.token";
        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractEmail(validToken)).thenReturn("user@test.com");
        when(userDetailsService.loadUserByUsername("user@test.com"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        Message<byte[]> message = createConnectMessage("Bearer " + validToken);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result, "Message should not be null (connection allowed despite missing user)");
    }

    @Test
    @DisplayName("Should allow CONNECT with valid JWT when extractEmail throws")
    void shouldAllowConnectWhenExtractEmailThrows() {
        String badToken = "expired.jwt.token";
        when(jwtService.validateToken(badToken)).thenReturn(false);

        Message<byte[]> message = createConnectMessage("Bearer " + badToken);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result, "Message should not be null (connection allowed despite bad token)");
        verify(jwtService, never()).extractEmail(anyString());
    }

    @Test
    @DisplayName("Should authenticate when valid JWT and user exists")
    void shouldAuthenticateWithValidJwt() {
        String validToken = "valid.jwt.token";
        String email = "user@test.com";
        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("password")
                .authorities("ROLE_USER")
                .build();

        when(jwtService.validateToken(validToken)).thenReturn(true);
        when(jwtService.extractEmail(validToken)).thenReturn(email);
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);

        Message<byte[]> message = createConnectMessage("Bearer " + validToken);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result, "Message should not be null");
        verify(jwtService).validateToken(validToken);
        verify(jwtService).extractEmail(validToken);
        verify(userDetailsService).loadUserByUsername(email);
    }

    @Test
    @DisplayName("Should not call extractEmail for non-Bearer auth header")
    void shouldIgnoreNonBearerAuthHeader() {
        Message<byte[]> message = createConnectMessage("Basic dXNlcjpwYXNz");

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verifyNoInteractions(jwtService);
    }
}
