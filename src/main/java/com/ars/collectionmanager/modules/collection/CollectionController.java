package com.ars.collectionmanager.modules.collection;

import com.ars.collectionmanager.core.GlobalExceptionHandler;
import com.ars.collectionmanager.modules.collection.dtos.CollectionLeftJoinRowResponse;
import com.ars.collectionmanager.modules.collection.dtos.CollectionResponse;
import com.ars.collectionmanager.modules.collection.dtos.CollectionWithItemsResponse;
import com.ars.collectionmanager.modules.collection.dtos.CreateCollectionDto;
import com.ars.collectionmanager.modules.collection.dtos.PatchCollectionDto;
import com.ars.collectionmanager.modules.collection.dtos.UpdateCollectionDto;
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
 * Endpoints de colecao - CRUD completo com soft delete, sempre do usuario autenticado.
 * POST   /collections                -> 201 (criar)
 * GET    /collections                -> 200 List (sem paginacao)
 * GET    /collections/paged          -> 200 Page (com paginacao)
 * GET    /collections/like           -> 200 List LIKE (sem paginacao)
 * GET    /collections/like/paged     -> 200 Page LIKE (com paginacao)
 * GET    /collections/with-relations -> 200 List collection -> items (forma aninhada)
 * GET    /collections/with-relations/{id} -> 200 CollectionWithItemsResponse
 * GET    /collections/left-join      -> 200 List linhas cruas collection|item (LEFT JOIN)
 * GET    /collections/left-join/{id} -> 200 List linhas cruas de 1 colecao
 * GET    /collections/specifications -> 200 List collection -> items (Specification programatica)
 * GET    /collections/specifications/{id} -> 200 CollectionWithItemsResponse
 * GET    /collections/{id}           -> 200 CollectionResponse
 * PUT    /collections/{id}           -> 200 CollectionResponse (update completo)
 * PATCH  /collections/{id}           -> 200 CollectionResponse (update parcial)
 * DELETE /collections/{id}           -> 204 (soft delete)
 *
 * Todos exigem "Authorization: Bearer <accessToken>".
 * O proprietario (user) vem sempre do token - nunca do body.
 */
@Tag(name = "Collections", description = "CRUD de colecoes do usuario autenticado, com soft delete e paginacao")
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    // ========== CREATE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Criar colecao", description = "Cria uma colecao para o usuario autenticado. Retorna 201", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Colecao criada"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou (nome/codigo/descricao)",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja cadastrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PostMapping
    public ResponseEntity<CollectionResponse> create(@Valid @RequestBody CreateCollectionDto dto) {
        CollectionResponse response = collectionService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== READ ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar minhas colecoes", description = "Lista as colecoes do usuario autenticado (sem paginacao)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de colecoes"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getAll() {
        return ResponseEntity.ok(collectionService.getAll());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar paginado", description = "Lista paginada das colecoes do usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de colecoes"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/paged")
    public ResponseEntity<Page<CollectionResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(collectionService.getAllPaged(pageable));
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
    public ResponseEntity<List<CollectionResponse>> getAllLike(@RequestParam String search) {
        return ResponseEntity.ok(collectionService.getAllLike(search));
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
    public ResponseEntity<Page<CollectionResponse>> getAllLikePaged(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(collectionService.getAllLikePaged(search, pageable));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar com items (aninhado)", description = "Lista minhas colecoes ativas com itens aninhados (1 query com LEFT JOIN FETCH, tipo relations do TypeORM)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de colecoes com itens"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/with-relations")
    public ResponseEntity<List<CollectionWithItemsResponse>> getAllWithRelations() {
        return ResponseEntity.ok(collectionService.getAllWithRelations());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar com items por id", description = "Retorna 1 colecao minha com itens aninhados", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecao com itens"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/with-relations/{id}")
    public ResponseEntity<CollectionWithItemsResponse> getByIdWithRelations(@PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getByIdWithRelations(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar com LEFT JOIN cru", description = "Uma linha por combinacao collection|item; item nulo quando nao existe (comportamento de JOIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linhas do LEFT JOIN"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/left-join")
    public ResponseEntity<List<CollectionLeftJoinRowResponse>> getAllLeftJoin() {
        return ResponseEntity.ok(collectionService.getAllLeftJoin());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "LEFT JOIN cru por id", description = "Linhas do LEFT JOIN de 1 colecao minha", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linhas do LEFT JOIN da colecao"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/left-join/{id}")
    public ResponseEntity<List<CollectionLeftJoinRowResponse>> getByIdLeftJoin(@PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getByIdLeftJoin(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar com Specification", description = "Mesmo resultado do with-relations, montado com Specification + root.fetch programatico (JpaSpecificationExecutor)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de colecoes com itens"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/specifications")
    public ResponseEntity<List<CollectionWithItemsResponse>> getAllWithSpecifications() {
        return ResponseEntity.ok(collectionService.getAllWithSpecifications());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar com Specification por id", description = "Mesmo resultado do with-relations/{id}, montado com Specification programatica", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecao com itens"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/specifications/{id}")
    public ResponseEntity<CollectionWithItemsResponse> getByIdWithSpecifications(@PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getByIdWithSpecifications(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar por id", description = "Retorna uma colecao do usuario autenticado pelo id", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecao encontrada"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<CollectionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getById(id));
    }

    // ========== UPDATE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar colecao", description = "Update completo (PUT) - todos os campos obrigatorios", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecao atualizada"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja cadastrado por outra colecao",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<CollectionResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateCollectionDto dto) {
        return ResponseEntity.ok(collectionService.update(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizacao parcial", description = "Update parcial (PATCH) - envie apenas os campos a alterar", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Colecao atualizada"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja cadastrado por outra colecao",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<CollectionResponse> partialUpdate(@PathVariable Long id,
                                                            @Valid @RequestBody PatchCollectionDto dto) {
        return ResponseEntity.ok(collectionService.partialUpdate(id, dto));
    }

    // ========== DELETE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir colecao", description = "Soft delete - o registro e marcado como removido, nao apagado. Retorna 204", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Colecao removida (soft delete)"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Colecao nao encontrada ou nao pertence ao usuario autenticado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        collectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
