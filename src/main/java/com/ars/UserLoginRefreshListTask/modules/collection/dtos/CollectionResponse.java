package com.ars.UserLoginRefreshListTask.modules.collection.dtos;

import java.time.LocalDateTime;

/**
 * Dados de saida da colecao - sempre expoe o proprietario (userId/userName).
 * Usado em todos os GETs, PUT e PATCH.
 */
public record CollectionResponse(
        Long id,
        String name,
        String code,
        String description,
        Long userId,
        String userName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
