package com.ars.UserLoginRefreshListTask.modules.user;

import com.ars.UserLoginRefreshListTask.modules.user.dtos.CreateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.PatchUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UpdateUserDto;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.UserResponse;
import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
