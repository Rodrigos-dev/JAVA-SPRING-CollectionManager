package com.ars.collectionmanager.modules.user;

import com.ars.collectionmanager.modules.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA do usuario.
 * Todos os metodos filtram deletedAt IS NULL (soft delete).
 * JpaSpecificationExecutor habilita o findAll(Specification) pras chamadas /specifications.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.id <> :id AND u.deletedAt IS NULL")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    Page<User> findAllActive(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<User> findAllActiveLike(@Param("search") String search);

    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findAllActiveLike(@Param("search") String search, Pageable pageable);

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndDeletedAtIsNull(@Param("email") String email);

    // ========== RELACIONAIS (user -> collections -> items) ==========

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.collections c LEFT JOIN FETCH c.items WHERE u.deletedAt IS NULL")
    List<User> findAllActiveWithRelations();

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.collections c LEFT JOIN FETCH c.items WHERE u.deletedAt IS NULL AND u.id = :id")
    Optional<User> findByIdActiveWithRelations(@Param("id") Long id);

    @Query("SELECT u, c, i FROM User u LEFT JOIN u.collections c LEFT JOIN c.items i WHERE u.deletedAt IS NULL")
    List<Object[]> findAllActiveLeftJoinRows();

    @Query("SELECT u, c, i FROM User u LEFT JOIN u.collections c LEFT JOIN c.items i WHERE u.deletedAt IS NULL AND u.id = :id")
    List<Object[]> findActiveLeftJoinRowsById(@Param("id") Long id);
}
