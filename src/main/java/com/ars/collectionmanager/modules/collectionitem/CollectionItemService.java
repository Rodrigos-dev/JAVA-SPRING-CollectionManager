package com.ars.collectionmanager.modules.collectionitem;

import com.ars.collectionmanager.modules.collection.CollectionRepository;
import com.ars.collectionmanager.modules.collection.entities.Collection;
import com.ars.collectionmanager.modules.collectionitem.dtos.CollectionItemResponse;
import com.ars.collectionmanager.modules.collectionitem.dtos.CreateCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.dtos.PatchCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.dtos.UpdateCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.entities.CollectionItem;
import com.ars.collectionmanager.modules.user.UserRepository;
import com.ars.collectionmanager.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servico do item de colecao: CRUD completo com soft delete.
 * O proprietario herda da Collection (nao tem FK propria): toda operacao
 * primeiro resolve a collection garantindo que ela pertence ao usuario autenticado.
 */
@Service
@RequiredArgsConstructor
public class CollectionItemService {

    private final CollectionItemRepository collectionItemRepository;
    private final CollectionRepository collectionRepository;
    private final UserRepository userRepository;

    // ========== CREATE ==========

    public CollectionItemResponse create(CreateCollectionItemDto dto) {
        Collection collection = findOwnedCollectionOrThrow(dto.collectionId());

        if (collectionItemRepository.existsByCollectionIdAndCode(collection.getId(), dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Item ja cadastrado com o codigo: " + dto.code() + " nesta colecao");
        }

        CollectionItem item = new CollectionItem();
        item.setName(dto.name());
        item.setCode(dto.code());
        item.setDescription(dto.description());
        item.setCollection(collection);

        CollectionItem saved = collectionItemRepository.save(item);

        return toResponse(saved);
    }

    // ========== READ ==========

    public List<CollectionItemResponse> getAll(Long collectionId) {
        if (collectionId != null) {
            findOwnedCollectionOrThrow(collectionId);
            return collectionItemRepository.findAllActiveByCollectionId(collectionId).stream()
                    .map(this::toResponse)
                    .toList();
        }
        return collectionItemRepository.findAllActiveByOwnerId(currentUser().getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<CollectionItemResponse> getAllPaged(Long collectionId, Pageable pageable) {
        if (collectionId != null) {
            findOwnedCollectionOrThrow(collectionId);
            return collectionItemRepository.findAllActiveByCollectionId(collectionId, pageable)
                    .map(this::toResponse);
        }
        return collectionItemRepository.findAllActiveByOwnerId(currentUser().getId(), pageable)
                .map(this::toResponse);
    }

    public List<CollectionItemResponse> getAllLike(String search) {
        return collectionItemRepository.findAllActiveLikeByOwnerId(currentUser().getId(), search).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<CollectionItemResponse> getAllLikePaged(String search, Pageable pageable) {
        return collectionItemRepository.findAllActiveLikeByOwnerId(currentUser().getId(), search, pageable)
                .map(this::toResponse);
    }

    public CollectionItemResponse getById(Long id) {
        return toResponse(findOwnedItemOrThrow(id));
    }

    // ========== UPDATE ==========

    public CollectionItemResponse update(Long id, UpdateCollectionItemDto dto) {
        CollectionItem item = findOwnedItemOrThrow(id);
        Long collectionId = item.getCollection().getId();

        if (!dto.code().equals(item.getCode())) {
            if (collectionItemRepository.existsByCollectionIdAndCodeAndIdNot(collectionId, dto.code(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Item ja cadastrado com o codigo: " + dto.code() + " nesta colecao");
            }
        }

        // PUT = substituicao completa (name/code obrigatorios via @NotBlank)
        item.setName(dto.name());
        item.setCode(dto.code());
        item.setDescription(dto.description());

        CollectionItem saved = collectionItemRepository.save(item);
        return toResponse(saved);
    }

    public CollectionItemResponse partialUpdate(Long id, PatchCollectionItemDto dto) {
        CollectionItem item = findOwnedItemOrThrow(id);
        Long collectionId = item.getCollection().getId();

        if (dto.code() != null && !dto.code().equals(item.getCode())) {
            if (collectionItemRepository.existsByCollectionIdAndCodeAndIdNot(collectionId, dto.code(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Item ja cadastrado com o codigo: " + dto.code() + " nesta colecao");
            }
        }

        // PATCH = atualizacao parcial (so o que vier nao-null)
        if (dto.name() != null) {
            item.setName(dto.name());
        }
        if (dto.code() != null) {
            item.setCode(dto.code());
        }
        if (dto.description() != null) {
            item.setDescription(dto.description());
        }

        CollectionItem saved = collectionItemRepository.save(item);
        return toResponse(saved);
    }

    // ========== DELETE (soft) ==========

    public void delete(Long id) {
        CollectionItem item = findOwnedItemOrThrow(id);
        item.setDeletedAt(LocalDateTime.now());
        collectionItemRepository.save(item);
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
    private Collection findOwnedCollectionOrThrow(Long id) {
        Collection collection = collectionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Colecao nao encontrada com id: " + id));

        if (!collection.getOwner().getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Colecao nao encontrada com id: " + id);
        }

        return collection;
    }

    /**
     * Busca o item garantindo que a colecao dona pertence ao usuario autenticado.
     */
    private CollectionItem findOwnedItemOrThrow(Long id) {
        CollectionItem item = collectionItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Item de colecao nao encontrado com id: " + id));

        if (!item.getCollection().getOwner().getId().equals(currentUser().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Item de colecao nao encontrado com id: " + id);
        }

        return item;
    }

    private CollectionItemResponse toResponse(CollectionItem item) {
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
}
