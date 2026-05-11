package com.nahora.dto.response;

import com.nahora.model.enums.TipoUsuario;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        TipoUsuario tipoUsuario
) {}