package com.ars.UserLoginRefreshListTask.modules.user.dtos;

import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CollectionWithItemsResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dados de saida do usuario com as colecoes e os itens aninhados
 * (forma "relations" tipo TypeORM: user -> collections -> items).
 * NUNCA retorna a senha.
 */
public record UserWithRelationsResponse(
        Long id,
        String name,
        String email,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CollectionWithItemsResponse> collections
) {
}
