package com.ars.collectionmanager.modules.collection.dtos;

import com.ars.collectionmanager.modules.collectionitem.dtos.CollectionItemResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dados de saida da colecao com os itens aninhados (forma "relations" tipo TypeORM).
 * Mesmo shape do CollectionResponse + lista de itens ativos.
 */
public record CollectionWithItemsResponse(
        Long id,
        String name,
        String code,
        String description,
        Long userId,
        String userName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CollectionItemResponse> items
) {
}
