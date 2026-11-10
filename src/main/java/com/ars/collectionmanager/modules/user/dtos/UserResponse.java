package com.ars.collectionmanager.modules.user.dtos;

import java.time.LocalDateTime;

/**
 * Dados de saida do usuario - NUNCA retorna a senha.
 * Usado em todos os GETs, PUT e PATCH.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
