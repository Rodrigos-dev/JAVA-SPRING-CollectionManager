package com.ars.UserLoginRefreshListTask.modules.user;

import com.ars.UserLoginRefreshListTask.modules.user.dtos.CreateUserRequest;
import com.ars.UserLoginRefreshListTask.modules.user.dtos.CreateUserResponse;
import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servico de usuario: cadastro basico (nome, email, senha, descricao).
 * Senha codificada com BCrypt - nunca texto puro.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CreateUserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Usuario ja cadastrado com o email: " + request.email());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setDescription(request.description());

        User saved = userRepository.save(user);

        return new CreateUserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getDescription());
    }
}
