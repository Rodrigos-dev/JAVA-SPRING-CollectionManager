package com.ars.UserLoginRefreshListTask.modules.user.dtos;

/**
 * Dados de saida do cadastro - NUNCA retorna a senha.
 */
public record CreateUserResponse(
        Long id,
        String name,
        String email,
        String description
) {
}
