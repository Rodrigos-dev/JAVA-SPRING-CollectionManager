package com.ars.UserLoginRefreshListTask.modules.user;

import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA do usuario.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
