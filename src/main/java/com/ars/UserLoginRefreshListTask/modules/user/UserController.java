package com.ars.UserLoginRefreshListTask.modules.user;

import com.ars.UserLoginRefreshListTask.core.GlobalExceptionHandler;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.CreateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.PatchUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UpdateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UserLeftJoinRowResponse;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UserResponse;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UserWithRelationsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * Endpoints de usuario - CRUD completo com soft delete.
 * POST   /users                    -> 201 (criar)
 * GET    /users                    -> 200 List (sem paginacao)
 * GET    /users/paged              -> 200 Page (com paginacao)
 * GET    /users/like               -> 200 List LIKE (sem paginacao)
 * GET    /users/like/paged         -> 200 Page LIKE (com paginacao)
 * GET    /users/query              -> 200 List query exata (sem paginacao)
 * GET    /users/query/paged        -> 200 Page query exata (com paginacao)
 * GET    /users/with-relations     -> 200 List user -> collections -> items (forma aninhada)
 * GET    /users/with-relations/{id}-> 200 UserWithRelationsResponse
 * GET    /users/left-join          -> 200 List linhas cruas user|collection|item (LEFT JOIN)
 * GET    /users/left-join/{id}     -> 200 List linhas cruas de 1 usuario
 * GET    /users/specifications     -> 200 List user -> collections -> items (Specification programatica)
 * GET    /users/specifications/{id}-> 200 UserWithRelationsResponse
 * GET    /users/{id}               -> 200 UserResponse
 * PUT    /users/{id}               -> 200 UserResponse (update completo)
 * PATCH  /users/{id}               -> 200 UserResponse (update parcial)
 * DELETE /users/{id}               -> 204 (soft delete)
 *
 * Todos exigem "Authorization: Bearer <accessToken>" (exceto POST /users).
 */
@Tag(name = "Users", description = "CRUD de usuarios com soft delete, paginacao e buscas")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ========== CREATE ==========

    @Operation(summary = "Criar usuario", description = "Cadastra um usuario novo (senha criptografada com BCrypt). Retorna 201")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou (nome/email/senha)",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserDto dto) {
        UserResponse response = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== READ ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar todos", description = "Lista completa de usuarios (sem paginacao)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar paginado", description = "Lista paginada com ordenacao configuravel", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de usuarios"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/paged")
    public ResponseEntity<Page<UserResponse>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getAllPaged(pageable));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca LIKE", description = "Busca por trecho em nome/email (sem paginacao)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada"),
            @ApiResponse(responseCode = "400", description = "Parametro 'search' ausente",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/like")
    public ResponseEntity<List<UserResponse>> getAllLike(@RequestParam String search) {
        return ResponseEntity.ok(userService.getAllLike(search));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca LIKE paginada", description = "Busca por trecho em nome/email com paginacao", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina filtrada"),
            @ApiResponse(responseCode = "400", description = "Parametro 'search' ausente",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/like/paged")
    public ResponseEntity<Page<UserResponse>> getAllLikePaged(
            @RequestParam String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getAllLikePaged(search, pageable));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca exata", description = "Filtra por name e/ou email exatos (sem paginacao)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista filtrada"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/query")
    public ResponseEntity<List<UserResponse>> getAllQuery(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(userService.getAllQuery(name, email));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca exata paginada", description = "Filtra por name e/ou email exatos com paginacao", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina filtrada"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/query/paged")
    public ResponseEntity<Page<UserResponse>> getAllQueryPaged(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(userService.getAllQueryPaged(name, email, pageable));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar com relations (aninhado)", description = "Lista todos os usuarios ativos com colecoes e itens aninhados (1 query com LEFT JOIN FETCH, tipo relations do TypeORM)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios com relations"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/with-relations")
    public ResponseEntity<List<UserWithRelationsResponse>> getAllWithRelations() {
        return ResponseEntity.ok(userService.getAllWithRelations());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar com relations por id", description = "Retorna 1 usuario com colecoes e itens aninhados", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario com relations"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/with-relations/{id}")
    public ResponseEntity<UserWithRelationsResponse> getByIdWithRelations(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getByIdWithRelations(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar com LEFT JOIN cru", description = "Uma linha por combinacao user|collection|item; collection/item nulos quando nao existe (comportamento de JOIN)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linhas do LEFT JOIN"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/left-join")
    public ResponseEntity<List<UserLeftJoinRowResponse>> getAllLeftJoin() {
        return ResponseEntity.ok(userService.getAllLeftJoin());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "LEFT JOIN cru por id", description = "Linhas do LEFT JOIN de 1 usuario", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Linhas do LEFT JOIN do usuario"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/left-join/{id}")
    public ResponseEntity<List<UserLeftJoinRowResponse>> getByIdLeftJoin(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getByIdLeftJoin(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar com Specification", description = "Mesmo resultado do with-relations, montado com Specification + root.fetch programatico (JpaSpecificationExecutor)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de usuarios com relations"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/specifications")
    public ResponseEntity<List<UserWithRelationsResponse>> getAllWithSpecifications() {
        return ResponseEntity.ok(userService.getAllWithSpecifications());
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar com Specification por id", description = "Mesmo resultado do with-relations/{id}, montado com Specification programatica", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario com relations"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/specifications/{id}")
    public ResponseEntity<UserWithRelationsResponse> getByIdWithSpecifications(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getByIdWithSpecifications(id));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar por id", description = "Retorna um usuario pelo id", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    // ========== UPDATE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizar usuario", description = "Update completo (PUT) - todos os campos obrigatorios", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado por outro usuario",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Atualizacao parcial", description = "Update parcial (PATCH) - envie apenas os campos a alterar", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
            @ApiResponse(responseCode = "400", description = "Validacao falhou",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado por outro usuario",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> partialUpdate(@PathVariable Long id,
                                                      @Valid @RequestBody PatchUserDto dto) {
        return ResponseEntity.ok(userService.partialUpdate(id, dto));
    }

    // ========== DELETE ==========

    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Excluir usuario", description = "Soft delete - o registro e marcado como removido, nao apagado. Retorna 204", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Usuario removido (soft delete)"),
            @ApiResponse(responseCode = "403", description = "Sem token, token invalido ou acesso negado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = GlobalExceptionHandler.ErrorBody.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
