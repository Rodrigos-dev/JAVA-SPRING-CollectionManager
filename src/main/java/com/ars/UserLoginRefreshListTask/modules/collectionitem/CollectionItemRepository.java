package com.ars.UserLoginRefreshListTask.modules.collectionitem;

import com.ars.UserLoginRefreshListTask.modules.collectionitem.entities.CollectionItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA do item de colecao.
 * Todos os metodos filtram deletedAt IS NULL (soft delete) e o dono
 * (owner da Collection - o item nao tem FK de usuario propria).
 */
public interface CollectionItemRepository extends JpaRepository<CollectionItem, Long> {

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM CollectionItem i WHERE i.collection.id = :collectionId AND i.code = :code AND i.deletedAt IS NULL")
    boolean existsByCollectionIdAndCode(@Param("collectionId") Long collectionId, @Param("code") String code);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM CollectionItem i WHERE i.collection.id = :collectionId AND i.code = :code AND i.id <> :id AND i.deletedAt IS NULL")
    boolean existsByCollectionIdAndCodeAndIdNot(@Param("collectionId") Long collectionId, @Param("code") String code, @Param("id") Long id);

    @Query("SELECT i FROM CollectionItem i WHERE i.deletedAt IS NULL AND i.collection.owner.id = :ownerId")
    List<CollectionItem> findAllActiveByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT i FROM CollectionItem i WHERE i.deletedAt IS NULL AND i.collection.owner.id = :ownerId")
    Page<CollectionItem> findAllActiveByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT i FROM CollectionItem i WHERE i.deletedAt IS NULL AND i.collection.owner.id = :ownerId AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<CollectionItem> findAllActiveLikeByOwnerId(@Param("ownerId") Long ownerId, @Param("search") String search);

    @Query("SELECT i FROM CollectionItem i WHERE i.deletedAt IS NULL AND i.collection.owner.id = :ownerId AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<CollectionItem> findAllActiveLikeByOwnerId(@Param("ownerId") Long ownerId, @Param("search") String search, Pageable pageable);

    @Query("SELECT i FROM CollectionItem i WHERE i.deletedAt IS NULL AND i.collection.id = :collectionId")
    List<CollectionItem> findAllActiveByCollectionId(@Param("collectionId") Long collectionId);

    @Query("SELECT i FROM CollectionItem i WHERE i.deletedAt IS NULL AND i.collection.id = :collectionId")
    Page<CollectionItem> findAllActiveByCollectionId(@Param("collectionId") Long collectionId, Pageable pageable);

    Optional<CollectionItem> findByIdAndDeletedAtIsNull(Long id);
}
