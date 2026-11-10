package com.ars.collectionmanager.modules.collectionitem;

import com.ars.collectionmanager.core.GlobalExceptionHandler;
import com.ars.collectionmanager.modules.collectionitem.dtos.CollectionItemResponse;
import com.ars.collectionmanager.modules.collectionitem.dtos.CreateCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.dtos.PatchCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.dtos.UpdateCollectionItemDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de item de colecao - CRUD completo com soft delete.
 * POST   /collection-items             -> 201 (criar, body com collectionId)
 * GET    /collection-items             -> 200 List (sem paginacao, collectionId opcional)
 * GET    /collection-items/paged       -> 200 Page (com paginacao)
 * GET    /collection-items/like        -> 200 List LIKE (sem paginacao)
 * GET    /collection-items/like/paged  -> 200 Page LIKE (com paginacao)
 * GET    /collection-items/{id}        -> 200 CollectionItemResponse
 * PUT    /collection-items/{id}        -> 200 CollectionItemResponse (update completo)
 * PATCH  /collection-items/{id}        -> 200 CollectionItemResponse (update parcial)
 * DELETE /collection-items/{id}        -> 204 (soft delete)
 *
 * Todos exigem "Authorization: Bearer <accessToken>".
 * O proprietario vem do owner da collection - nunca do body.
 */
@Tag(name = "Collection Items", description = "CRUD de itens das colecoes do usuario autenticado, com soft delete e paginacao")
@RestController
@RequestMapping("/collection-items")
@RequiredArgsConstructor
public class CollectionItemController {

    private final CollectionItemService collectionItemService;

    // ========== CREATE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar item de colecao", description = "Cria um item para uma colecao do usuario autenticado. Retorna 201", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item criado"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou (nome/codigo/descricao/collectionId)",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja cadastrado nesta colecao",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PostMapping
    public ResponseEntity<CollectionItemResponse> create(@Valid @RequestBody CreateCollectionItemDto dto) {
        CollectionItemResponse response = collectionItemService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== READ ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar meus itens", description = "Lista os itens das colecoes do usuario autenticado (sem paginacao). collectionId opcional para filtrar por colecao", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de itens"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao informada nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping
    public ResponseEntity<List<CollectionItemResponse>> getAll(
            @RequestParam(required = false) Long collectionId) {
        return ResponseEntity.ok(collectionItemService.getAll(collectionId));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar paginado", description = "Lista paginada dos itens das colecoes do usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de itens"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao informada nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/paged")
    public ResponseEntity<Page<CollectionItemResponse>> getAllPaged(
            @RequestParam(required = false) Long collectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(collectionItemService.getAllPaged(collectionId, pageable));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca LIKE", description = "Busca por trecho em nome/codigo (sem paginacao)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada"),
            @ApiResponse(responseCode = "400", description = "Parametro 'search' ausente",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/like")
    public ResponseEntity<List<CollectionItemResponse>> getAllLike(@RequestParam String search) {
        return ResponseEntity.ok(collectionItemService.getAllLike(search));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca LIKE paginada", description = "Busca por trecho em nome/codigo com paginacao", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina filtrada"),
            @ApiResponse(responseCode = "400", description = "Parametro 'search' ausente",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/like/paged")
    public ResponseEntity<Page<CollectionItemResponse>> getAllLikePaged(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(collectionItemService.getAllLikePaged(search, pageable));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar por id", description = "Retorna um item das colecoes do usuario autenticado pelo id", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Item nao encontrado ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CollectionItemResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(collectionItemService.getById(id));
    }

    // ========== UPDATE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar item", description = "Update completo (PUT) - todos os campos obrigatorios. A colecao dona nao muda", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Item nao encontrado ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja cadastrado nesta colecao",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CollectionItemResponse> update(@PathVariable Long id,
                                                         @Valid @RequestBody UpdateCollectionItemDto dto) {
        return ResponseEntity.ok(collectionItemService.update(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizacao parcial", description = "Update parcial (PATCH) - envie apenas os campos a alterar. A colecao dona nao muda", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Item nao encontrado ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja cadastrado nesta colecao",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<CollectionItemResponse> partialUpdate(@PathVariable Long id,
                                                                @Valid @RequestBody PatchCollectionItemDto dto) {
        return ResponseEntity.ok(collectionItemService.partialUpdate(id, dto));
    }

    // ========== DELETE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir item", description = "Soft delete - o registro e marcado como removido, nao apagado. Retorna 204", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido (soft delete)"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Item nao encontrado ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        collectionItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
