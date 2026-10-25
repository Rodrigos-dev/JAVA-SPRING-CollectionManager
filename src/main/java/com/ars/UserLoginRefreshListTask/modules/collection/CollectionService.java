package com.ars.UserLoginRefreshListTask.modules.collection;

import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CollectionResponse;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CreateCollectionDto;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.PatchCollectionDto;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.UpdateCollectionDto;
import com.ars.UserLoginRefreshListTask.modules.collection.entities.Collection;
import com.ars.UserLoginRefreshListTask.modules.user.UserRepository;
import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
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
}
