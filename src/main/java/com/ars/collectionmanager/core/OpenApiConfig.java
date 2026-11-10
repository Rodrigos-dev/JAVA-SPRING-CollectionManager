package com.ars.collectionmanager.core;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracao do Swagger / OpenAPI 3 (springdoc).
 * - Informacoes gerais da API (titulo, versao, descricao)
 * - Scheme Bearer JWT: habilita o botao "Authorize" no Swagger UI
 *   para testar endpoints autenticados sem sair do navegador
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Collection Manager API",
                version = "0.0.1",
                description = """
                        API de usuarios com autenticacao JWT (access + refresh token).

                        **Fluxo de autenticacao:**
                        1. `POST /auth/login` -> retorna accessToken + refreshToken
                        2. Envie `Authorization: Bearer <accessToken>` nas demais requisicoes
                        3. `POST /auth/refresh` -> emite novos tokens a partir de um refresh token valido

                        **Endpoints de usuario:** CRUD completo com soft delete, listagem paginada,
                        busca LIKE e busca por query exata.
                        """
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Cole o accessToken retornado por POST /auth/login"
)
public class OpenApiConfig {
}
