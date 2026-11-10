package com.ars.collectionmanager.modules.collectionitem.dtos;

import java.time.LocalDateTime;

/**
 * Dados de saida do item de colecao - expoe a colecao dona
 * (collectionId/collectionName) e o proprietario herdado (userId/userName).
 * Usado em todos os GETs, PUT e PATCH.
 */
public record CollectionItemResponse(
        Long id,
        String name,
        String code,
        String description,
        Long collectionId,
        String collectionName,
        Long userId,
        String userName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
