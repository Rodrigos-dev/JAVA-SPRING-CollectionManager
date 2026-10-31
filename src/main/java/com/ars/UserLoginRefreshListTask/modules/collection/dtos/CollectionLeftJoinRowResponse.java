package com.ars.UserLoginRefreshListTask.modules.collection.dtos;

import com.ars.UserLoginRefreshListTask.modules.collectionitem.dtos.CollectionItemResponse;

/**
 * Linha crua do LEFT JOIN collection -> item (forma "left-join").
 * Uma linha por combinacao; item = null quando a colecao nao tem item ativo.
 * O usuario aparece repetido quando tem varias linhas (comportamento de JOIN).
 */
public record CollectionLeftJoinRowResponse(
        CollectionResponse collection,
        CollectionItemResponse item
) {
}
