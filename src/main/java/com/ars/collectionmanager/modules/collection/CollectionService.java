package com.ars.collectionmanager.modules.collection;

import com.ars.collectionmanager.modules.collection.dtos.CollectionLeftJoinRowResponse;
import com.ars.collectionmanager.modules.collection.dtos.CollectionResponse;
import com.ars.collectionmanager.modules.collection.dtos.CollectionWithItemsResponse;
import com.ars.collectionmanager.modules.collection.dtos.CreateCollectionDto;
import com.ars.collectionmanager.modules.collection.dtos.PatchCollectionDto;
import com.ars.collectionmanager.modules.collection.dtos.UpdateCollectionDto;
import com.ars.collectionmanager.modules.collection.entities.Collection;
import com.ars.collectionmanager.modules.collectionitem.dtos.CollectionItemResponse;
import com.ars.collectionmanager.modules.collectionitem.entities.CollectionItem;
import com.ars.collectionmanager.modules.user.UserRepository;
import com.ars.collectionmanager.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.JoinType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servico de colecao: CRUD completo com soft delete, sempre do usuario autenticado.
 * Proprietario vem do token JWT (email no SecurityContext), nunca do body.
 */
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;

    // ========== CREATE ==========

    public CollectionResponse create(CreateCollectionDto dto) {
        User owner = currentUser();

        if (collectionRepository.existsByCode(dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Colecao ja cadastrada com o codigo: " + dto.code());
        }

        Collection collection = new Collection();
        collection.setName(dto.name());
        collection.setCode(dto.code());
        collection.setDescription(dto.description());
        collection.setOwner(owner);

        Collection saved = collectionRepository.save(collection);

        return toResponse(saved);
    }

    // ========== READ ==========

    public List<CollectionResponse> getAll() {
        return collectionRepository.findAllActiveByOwnerId(currentUser().getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<CollectionResponse> getAllPaged(Pageable pageable) {
        return collectionRepository.findAllActiveByOwnerId(currentUser().getId(), pageable)
                .map(this::toResponse);
    }

    public List<CollectionResponse> getAllLike(String search) {
        return collectionRepository.findAllActiveLikeByOwnerId(currentUser().getId(), search).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<CollectionResponse> getAllLikePaged(String search, Pageable pageable) {
        return collectionRepository.findAllActiveLikeByOwnerId(currentUser().getId(), search, pageable)
                .map(this::toResponse);
    }

    public CollectionResponse getById(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    // ========== RELACIONAIS (collection -> items) ==========

    public List<CollectionWithItemsResponse> getAllWithRelations() {
        return collectionRepository.findAllActiveWithRelationsByOwnerId(currentUser().getId()).stream()
                .map(this::toWithItems)
                .toList();
    }

    public CollectionWithItemsResponse getByIdWithRelations(Long id) {
        Collection collection = collectionRepository.findByIdActiveWithRelations(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Colecao nao encontrada com id: " + id));

        if (!collection.getOwner().getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Colecao nao encontrada com id: " + id);
        }

        return toWithItems(collection);
    }

    public List<CollectionLeftJoinRowResponse> getAllLeftJoin() {
        return collectionRepository.findAllActiveLeftJoinRowsByOwnerId(currentUser().getId()).stream()
                .map(this::toLeftJoinRow)
                .toList();
    }

    public List<CollectionLeftJoinRowResponse> getByIdLeftJoin(Long id) {
        List<Object[]> rows = collectionRepository.findActiveLeftJoinRowsById(id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Colecao nao encontrada com id: " + id);
        }

        Collection collection = (Collection) rows.get(0)[0];
        if (!collection.getOwner().getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Colecao nao encontrada com id: " + id);
        }

        return rows.stream()
                .map(this::toLeftJoinRow)
                .toList();
    }

    // ========== SPECIFICATIONS (mesmo resultado do with-relations, join programatico) ==========

    public List<CollectionWithItemsResponse> getAllWithSpecifications() {
        return collectionRepository.findAll(myCollectionsWithItemsSpec()).stream()
                .map(this::toWithItems)
                .toList();
    }

    public CollectionWithItemsResponse getByIdWithSpecifications(Long id) {
        Collection collection = collectionRepository.findAll(collectionByIdWithItemsSpec(id)).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Colecao nao encontrada com id: " + id));
        return toWithItems(collection);
    }

    /**
     * Specification "fluente" equivalente ao LEFT JOIN FETCH do JPQL:
     * root.fetch("items", LEFT) e o join programatico + o escopo do dono no where.
     */
    private Specification<Collection> myCollectionsWithItemsSpec() {
        return (root, query, cb) -> {
            query.distinct(true);
            root.fetch("items", JoinType.LEFT);
            return cb.and(
                    cb.isNull(root.get("deletedAt")),
                    cb.equal(root.get("owner").get("id"), currentUser().getId()));
        };
    }

    private Specification<Collection> collectionByIdWithItemsSpec(Long id) {
        return (root, query, cb) -> {
            query.distinct(true);
            root.fetch("items", JoinType.LEFT);
            return cb.and(
                    cb.isNull(root.get("deletedAt")),
                    cb.equal(root.get("id"), id),
                    cb.equal(root.get("owner").get("id"), currentUser().getId()));
        };
    }

    // ========== UPDATE ==========

    public CollectionResponse update(Long id, UpdateCollectionDto dto) {
        Collection collection = findOwnedOrThrow(id);

        if (!dto.code().equals(collection.getCode())) {
            if (collectionRepository.existsByCodeAndIdNot(dto.code(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Colecao ja cadastrada com o codigo: " + dto.code());
            }
        }

        // PUT = substituicao completa (name/code obrigatorios via @NotBlank)
        collection.setName(dto.name());
        collection.setCode(dto.code());
        collection.setDescription(dto.description());

        Collection saved = collectionRepository.save(collection);
        return toResponse(saved);
    }

    public CollectionResponse partialUpdate(Long id, PatchCollectionDto dto) {
        Collection collection = findOwnedOrThrow(id);

        if (dto.code() != null && !dto.code().equals(collection.getCode())) {
            if (collectionRepository.existsByCodeAndIdNot(dto.code(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Colecao ja cadastrada com o codigo: " + dto.code());
            }
        }

        // PATCH = atualizacao parcial (so o que vier nao-null)
        if (dto.name() != null) {
            collection.setName(dto.name());
        }
        if (dto.code() != null) {
            collection.setCode(dto.code());
        }
        if (dto.description() != null) {
            collection.setDescription(dto.description());
        }

        Collection saved = collectionRepository.save(collection);
        return toResponse(saved);
    }

    // ========== DELETE (soft) ==========

    public void delete(Long id) {
        Collection collection = findOwnedOrThrow(id);
        collection.setDeletedAt(LocalDateTime.now());
        collectionRepository.save(collection);
    }

    // ========== HELPERS ==========

    /**
     * Usuario autenticado a partir do email do JWT (principal do SecurityContext).
     */
    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token invalido ou ausente");
        }

        return userRepository.findByEmailAndDeletedAtIsNull(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Usuario autenticado nao encontrado"));
    }

    /**
     * Busca a colecao garantindo que pertence ao usuario autenticado.
     */
    private Collection findOwnedOrThrow(Long id) {
        Collection collection = collectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Colecao nao encontrada com id: " + id));

        if (!collection.getOwner().getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Colecao nao encontrada com id: " + id);
        }

        return collection;
    }

    private CollectionResponse toResponse(Collection collection) {
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

    private CollectionWithItemsResponse toWithItems(Collection collection) {
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

    /**
     * Monta uma linha do LEFT JOIN cru: collection sempre presente (LEFT),
     * item nulo quando nao existe (ou soft-deleted).
     */
    private CollectionLeftJoinRowResponse toLeftJoinRow(Object[] row) {
        Collection collection = (Collection) row[0];
        CollectionItem item = (CollectionItem) row[1];

        CollectionItemResponse itemResponse =
                (item == null || item.getDeletedAt() != null) ? null : toItemResponse(item);

        return new CollectionLeftJoinRowResponse(toResponse(collection), itemResponse);
    }
}
