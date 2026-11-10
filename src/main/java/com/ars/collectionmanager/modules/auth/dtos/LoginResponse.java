package com.ars.collectionmanager.modules.auth.dtos;

/**
 * Dados de saida do login/refresh.
 * Senha NUNCA aparece aqui.
 */
public record LoginResponse(
        String name,
        String email,
        String role,
        String accessToken,
        String refreshToken
) {
}
