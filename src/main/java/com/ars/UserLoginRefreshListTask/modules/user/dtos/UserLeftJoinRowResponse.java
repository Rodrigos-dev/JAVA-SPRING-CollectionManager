package com.ars.UserLoginRefreshListTask.modules.user.dtos;

import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CollectionResponse;
import com.ars.UserLoginRefreshListTask.modules.collectionitem.dtos.CollectionItemResponse;

/**
 * Linha crua do LEFT JOIN user -> collection -> item (forma "left-join").
 * Uma linha por combinacao; collection/item = null quando nao existe.
 * O usuario aparece repetido quando tem varias linhas (comportamento de JOIN).
 */
public record UserLeftJoinRowResponse(
        UserResponse user,
        CollectionResponse collection,
        CollectionItemResponse item
) {
}
