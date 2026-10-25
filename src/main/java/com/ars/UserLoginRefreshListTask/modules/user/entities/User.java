package com.ars.UserLoginRefreshListTask.modules.user.entities;

import com.ars.UserLoginRefreshListTask.modules.collection.entities.Collection;
import com.ars.UserLoginRefreshListTask.modules.user.enums.UserRoleEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade do usuario (tabela users - "user" e palavra reservada do MySQL).
 * deletedAt = soft delete (null = ativo, data = excluido).
 * Relacionamento: user (1) -> collections (N).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UserRoleEnum role = UserRoleEnum.USER;

    /** Lado 1 do relacionamento (1 -> N). Mapeado pelo campo owner de Collection. */
    @OneToMany(mappedBy = "owner")
    private List<Collection> collections;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
