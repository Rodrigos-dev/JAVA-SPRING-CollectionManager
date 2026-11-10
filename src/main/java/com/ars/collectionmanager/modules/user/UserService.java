package com.ars.collectionmanager.modules.user;

import com.ars.collectionmanager.modules.collection.dtos.CollectionResponse;
import com.ars.collectionmanager.modules.collection.dtos.CollectionWithItemsResponse;
import com.ars.collectionmanager.modules.collection.entities.Collection;
import com.ars.collectionmanager.modules.collectionitem.dtos.CollectionItemResponse;
import com.ars.collectionmanager.modules.collectionitem.entities.CollectionItem;
import com.ars.collectionmanager.modules.user.dtos.CreateUserDto;
import com.ars.collectionmanager.modules.user.dtos.PatchUserDto;
import com.ars.collectionmanager.modules.user.dtos.UpdateUserDto;
import com.ars.collectionmanager.modules.user.dtos.UserLeftJoinRowResponse;
import com.ars.collectionmanager.modules.user.dtos.UserResponse;
import com.ars.collectionmanager.modules.user.dtos.UserWithRelationsResponse;
import com.ars.collectionmanager.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.JoinType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servico de usuario: CRUD completo com soft delete.
 * Senha codificada com BCrypt - nunca texto puro.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ========== CREATE ==========

    public UserResponse create(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Usuario ja cadastrado com o email: " + dto.email());
        }

        User user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setDescription(dto.description());

        User saved = userRepository.save(user);

        return toResponse(saved);
    }

    // ========== READ ==========

    public List<UserResponse> getAll() {
        return userRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<UserResponse> getAllPaged(Pageable pageable) {
        return userRepository.findAllActive(pageable)
                .map(this::toResponse);
    }

    public List<UserResponse> getAllLike(String search) {
        return userRepository.findAllActiveLike(search).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<UserResponse> getAllLikePaged(String search, Pageable pageable) {
        return userRepository.findAllActiveLike(search, pageable)
                .map(this::toResponse);
    }

    public List<UserResponse> getAllQuery(String name, String email) {
        User probe = buildProbe(name, email);
        return userRepository.findAll(Example.of(probe, defaultMatcher())).stream()
                .filter(u -> u.getDeletedAt() == null)
                .map(this::toResponse)
                .toList();
    }

    public Page<UserResponse> getAllQueryPaged(String name, String email, Pageable pageable) {
        User probe = buildProbe(name, email);
        return userRepository.findAll(Example.of(probe, defaultMatcher()), pageable)
                .map(u -> {
                    if (u.getDeletedAt() != null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
                    }
                    return toResponse(u);
                });
    }

    public UserResponse getById(Long id) {
        User user = findUserOrThrow(id);
        return toResponse(user);
    }

    // ========== RELACIONAIS (user -> collections -> items) ==========

    public List<UserWithRelationsResponse> getAllWithRelations() {
        return userRepository.findAllActiveWithRelations().stream()
                .map(this::toWithRelations)
                .toList();
    }

    public UserWithRelationsResponse getByIdWithRelations(Long id) {
        User user = userRepository.findByIdActiveWithRelations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario nao encontrado com id: " + id));
        return toWithRelations(user);
    }

    public List<UserLeftJoinRowResponse> getAllLeftJoin() {
        return userRepository.findAllActiveLeftJoinRows().stream()
                .map(this::toLeftJoinRow)
                .toList();
    }

    public List<UserLeftJoinRowResponse> getByIdLeftJoin(Long id) {
        List<Object[]> rows = userRepository.findActiveLeftJoinRowsById(id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Usuario nao encontrado com id: " + id);
        }
        return rows.stream()
                .map(this::toLeftJoinRow)
                .toList();
    }

    // ========== SPECIFICATIONS (mesmo resultado do with-relations, join programatico) ==========

    public List<UserWithRelationsResponse> getAllWithSpecifications() {
        return userRepository.findAll(usersWithRelationsSpec()).stream()
                .map(this::toWithRelations)
                .toList();
    }

    public UserWithRelationsResponse getByIdWithSpecifications(Long id) {
        User user = userRepository.findAll(userByIdWithRelationsSpec(id)).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario nao encontrado com id: " + id));
        return toWithRelations(user);
    }

    /**
     * Specification "fluente" equivalente ao LEFT JOIN FETCH do JPQL:
     * root.fetch("collections", LEFT).fetch("items", LEFT) e o join programatico.
     */
    private Specification<User> usersWithRelationsSpec() {
        return (root, query, cb) -> {
            query.distinct(true);
            root.fetch("collections", JoinType.LEFT).fetch("items", JoinType.LEFT);
            return cb.isNull(root.get("deletedAt"));
        };
    }

    private Specification<User> userByIdWithRelationsSpec(Long id) {
        return (root, query, cb) -> {
            query.distinct(true);
            root.fetch("collections", JoinType.LEFT).fetch("items", JoinType.LEFT);
            return cb.and(
                    cb.equal(root.get("id"), id),
                    cb.isNull(root.get("deletedAt")));
        };
    }

    // ========== UPDATE ==========

    public UserResponse update(Long id, UpdateUserDto dto) {
        User user = findUserOrThrow(id);

        if (!dto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Usuario ja cadastrado com o email: " + dto.email());
            }
        }

        // PUT = substituicao completa (name/email/description obrigatorios via @NotBlank)
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setDescription(dto.description());
        if (dto.password() != null) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    public UserResponse partialUpdate(Long id, PatchUserDto dto) {
        User user = findUserOrThrow(id);

        if (dto.email() != null && !dto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Usuario ja cadastrado com o email: " + dto.email());
            }
        }

        // PATCH = atualizacao parcial (so o que vier nao-null)
        if (dto.name() != null) {
            user.setName(dto.name());
        }
        if (dto.email() != null) {
            user.setEmail(dto.email());
        }
        if (dto.password() != null) {
            user.setPassword(passwordEncoder.encode(dto.password()));
        }
        if (dto.description() != null) {
            user.setDescription(dto.description());
        }

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    // ========== DELETE (soft) ==========

    public void delete(Long id) {
        User user = findUserOrThrow(id);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    // ========== HELPERS ==========

    private User findUserOrThrow(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario nao encontrado com id: " + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDescription(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private UserWithRelationsResponse toWithRelations(User user) {
        return new UserWithRelationsResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDescription(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getCollections().stream()
                        .filter(c -> c.getDeletedAt() == null)
                        .map(this::toCollectionWithItems)
                        .toList()
        );
    }

    private CollectionWithItemsResponse toCollectionWithItems(Collection collection) {
        return new CollectionWithItemsResponse(
                collection.getId(),
                collection.getName(),
                collection.getCode(),
                collection.getDescription(),
                collection.getOwner().getId(),
                collection.getOwner().getName(),
                collection.getCreatedAt(),
                collection.getUpdatedAt(),
                collection.getItems().stream()
                        .filter(i -> i.getDeletedAt() == null)
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private CollectionItemResponse toItemResponse(CollectionItem item) {
        return new CollectionItemResponse(
                item.getId(),
                item.getName(),
                item.getCode(),
                item.getDescription(),
                item.getCollection().getId(),
                item.getCollection().getName(),
                item.getCollection().getOwner().getId(),
                item.getCollection().getOwner().getName(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private CollectionResponse toCollectionResponse(Collection collection) {
        return new CollectionResponse(
                collection.getId(),
                collection.getName(),
                collection.getCode(),
                collection.getDescription(),
                collection.getOwner().getId(),
                collection.getOwner().getName(),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }

    /**
     * Monta uma linha do LEFT JOIN cru: user sempre presente (LEFT),
     * collection/item nulos quando nao existe (ou soft-deleted).
     */
    private UserLeftJoinRowResponse toLeftJoinRow(Object[] row) {
        User user = (User) row[0];
        Collection collection = (Collection) row[1];
        CollectionItem item = (CollectionItem) row[2];

        CollectionResponse collectionResponse =
                (collection == null || collection.getDeletedAt() != null) ? null : toCollectionResponse(collection);
        CollectionItemResponse itemResponse =
                (item == null || item.getDeletedAt() != null) ? null : toItemResponse(item);

        return new UserLeftJoinRowResponse(toResponse(user), collectionResponse, itemResponse);
    }

    private User buildProbe(String name, String email) {
        User probe = new User();
        probe.setName(name);
        probe.setEmail(email);
        return probe;
    }

    private ExampleMatcher defaultMatcher() {
        return ExampleMatcher.matching()
                .withIgnoreNullValues()
                .withIgnoreCase();
    }
}
