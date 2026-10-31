package com.ars.UserLoginRefreshListTask.modules.collection.entities;

import com.ars.UserLoginRefreshListTask.modules.collectionitem.entities.CollectionItem;
import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade colecao do usuario (tabela collections).
 * Relacionamento: user (1) -> collections (N), FK em user_id.
 * deletedAt = soft delete (null = ativo, data = excluido) - mesmo padrao de User.
 */
@Entity
@Table(name = "collections")
@Getter
@Setter
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(length = 500)
    private String description;

    /**
     * Proprietario da colecao. Fetch EAGER (padrao do ManyToOne) para que a
     * resposta possa ler owner.getId()/getName() sem sessao aberta.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    /**
     * Itens da colecao (1 -> N). LAZY (padrao do OneToMany) - nunca entra
     * nas respostas, que expem apenas dados da propria colecao.
     */
    @OneToMany(mappedBy = "collection")
    private List<CollectionItem> items;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;
}
