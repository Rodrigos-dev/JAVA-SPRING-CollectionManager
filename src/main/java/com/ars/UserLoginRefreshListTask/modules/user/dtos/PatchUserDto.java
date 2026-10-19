package com.ars.UserLoginRefreshListTask.modules.user.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada do PATCH (atualizacao PARCIAL - REST estrito).
 * Todos os campos opcionais - so atualiza o que for informado (nao-null).
 * Regras INDEPENDENTES dos outros DTOs.
 */
public record PatchUserDto(
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @Email(message = "Email invalido")
        String email,

        @Size(min = 6, max = 72, message = "Senha deve ter entre 6 e 72 caracteres")
        String password,

        @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
        String description
) {
}
