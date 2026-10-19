package com.ars.UserLoginRefreshListTask.modules.user;

import com.ars.UserLoginRefreshListTask.modules.user.dtos.CreateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.PatchUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UpdateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * GET    /users/{id}               -> 200 UserResponse
 * PUT    /users/{id}               -> 200 UserResponse (update completo)
 * PATCH  /users/{id}               -> 200 UserResponse (update parcial)
 * DELETE /users/{id}               -> 204 (soft delete)
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ========== CREATE ==========

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserDto dto) {
        UserResponse response = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== READ ==========

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

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

    @GetMapping("/like")
    public ResponseEntity<List<UserResponse>> getAllLike(@RequestParam String search) {
        return ResponseEntity.ok(userService.getAllLike(search));
    }

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

    @GetMapping("/query")
    public ResponseEntity<List<UserResponse>> getAllQuery(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(userService.getAllQuery(name, email));
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    // ========== UPDATE ==========

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateUserDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> partialUpdate(@PathVariable Long id,
                                                      @Valid @RequestBody PatchUserDto dto) {
        return ResponseEntity.ok(userService.partialUpdate(id, dto));
    }

    // ========== DELETE ==========

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
