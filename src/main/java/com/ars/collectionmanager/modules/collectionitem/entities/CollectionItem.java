package com.ars.collectionmanager.modules.collectionitem.entities;

import com.ars.collectionmanager.modules.collection.entities.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidade item da colecao (tabela collection_items).
 * Relacionamento: collections (1) -> collection_items (N), FK em collection_id.
 * O proprietario nao tem FK propria - herda do owner da Collection (evita duplicidade).
 * deletedAt = soft delete (null = ativo, data = excluido) - mesmo padrao de User e Collection.
 */
@Entity
@Table(name = "collection_items")
@Getter
@Setter
public class CollectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(length = 500)
    private String description;

    /**
     * Colecao dona do item. Fetch EAGER (padrao do ManyToOne) para que a
     * resposta possa ler collection.getId()/getName()/getOwner() sem sessao aberta.
     * Unicidade do code e por colecao (constraint logica no service).
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "collection_id", nullable = false)
    private Collection collection;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
