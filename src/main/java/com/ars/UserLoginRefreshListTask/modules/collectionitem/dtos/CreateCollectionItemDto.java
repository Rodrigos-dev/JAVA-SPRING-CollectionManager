package com.ars.UserLoginRefreshListTask.modules.collectionitem.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada da criacao de item de colecao.
 * O proprietario NAO vem no body - vem da Collection informada em collectionId,
 * cujo owner precisa ser o usuario autenticado.
 */
public record CreateCollectionItemDto(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotBlank(message = "Codigo e obrigatorio")
        @Size(min = 2, max = 30, message = "Codigo deve ter entre 2 e 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Codigo deve conter apenas letras, numeros, hifen ou underline")
        String code,

        @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
        String description,

        @NotNull(message = "Colecao e obrigatoria")
        Long collectionId
) {
}
