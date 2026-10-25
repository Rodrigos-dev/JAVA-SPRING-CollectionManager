package com.ars.UserLoginRefreshListTask.modules.collection;

import com.ars.UserLoginRefreshListTask.modules.collection.entities.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA da colecao.
 * Todos os metodos filtram deletedAt IS NULL (soft delete) e o dono (owner).
 */
public interface CollectionRepository extends JpaRepository<Collection, Long> {

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Collection c WHERE c.code = :code AND c.deletedAt IS NULL")
    boolean existsByCode(@Param("code") String code);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Collection c WHERE c.code = :code AND c.id <> :id AND c.deletedAt IS NULL")
    boolean existsByCodeAndIdNot(@Param("code") String code, @Param("id") Long id);

    @Query("SELECT c FROM Collection c WHERE c.deletedAt IS NULL AND c.owner.id = :ownerId")
    List<Collection> findAllActiveByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Collection c WHERE c.deletedAt IS NULL AND c.owner.id = :ownerId")
    Page<Collection> findAllActiveByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT c FROM Collection c WHERE c.deletedAt IS NULL AND c.owner.id = :ownerId AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Collection> findAllActiveLikeByOwnerId(@Param("ownerId") Long ownerId, @Param("search") String search);

    @Query("SELECT c FROM Collection c WHERE c.deletedAt IS NULL AND c.owner.id = :ownerId AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Collection> findAllActiveLikeByOwnerId(@Param("ownerId") Long ownerId, @Param("search") String search, Pageable pageable);

    Optional<Collection> findByIdAndDeletedAtIsNull(Long id);
}
