package com.ars.collectionmanager.modules.collection.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada do update completo (PUT) - campos obrigatorios.
 */
public record UpdateCollectionDto(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotBlank(message = "Codigo e obrigatorio")
        @Size(min = 2, max = 30, message = "Codigo deve ter entre 2 e 30 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Codigo deve conter apenas letras, numeros, hifen ou underline")
        String code,

        @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
        String description
) {
}
