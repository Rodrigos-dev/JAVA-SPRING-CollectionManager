package com.ars.collectionmanager.modules.user;

import com.ars.collectionmanager.modules.collection.entities.Collection;
import com.ars.collectionmanager.modules.collectionitem.entities.CollectionItem;
import com.ars.collectionmanager.modules.user.dtos.CreateUserDto;
import com.ars.collectionmanager.modules.user.dtos.PatchUserDto;
import com.ars.collectionmanager.modules.user.dtos.UpdateUserDto;
import com.ars.collectionmanager.modules.user.dtos.UserLeftJoinRowResponse;
import com.ars.collectionmanager.modules.user.dtos.UserResponse;
import com.ars.collectionmanager.modules.user.dtos.UserWithRelationsResponse;
import com.ars.collectionmanager.modules.user.entities.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Specification> specCaptor;

    @Mock
    private Root<User> root;

    @Mock
    private CriteriaQuery query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Fetch fetch;

    // ========== CREATE ==========

    @Test
    void create_deveCriarUsuarioComSenhaCriptografada_quandoEmailNaoExiste() {
        CreateUserDto dto = new CreateUserDto("Ana Silva", "ana@email.com", "senha123", "dev");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        stubSave();

        UserResponse response = userService.create(dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ana Silva");
        assertThat(response.email()).isEqualTo("ana@email.com");
        assertThat(response.description()).isEqualTo("dev");
        assertThat(saved.getPassword()).isNotEqualTo("senha123");
        assertThat(saved.getPassword()).startsWith("$2a$");
    }

    @Test
    void create_deveRetornar409_quandoEmailJaCadastrado() {
        CreateUserDto dto = new CreateUserDto("Ana Silva", "ana@email.com", "senha123", "dev");

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.create(dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ========== READ ==========

    @Test
    void getAll_deveListarTodosMapeadosParaResponse() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        User bia = buildUser(2L, "Bia Souza", "bia@email.com");
        when(userRepository.findAllActive()).thenReturn(List.of(ana, bia));

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("Ana Silva");
        assertThat(result.get(1).email()).isEqualTo("bia@email.com");
    }

    @Test
    void getAllPaged_deveRetornarPageMapeada() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findAllActive(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ana)));

        Page<UserResponse> result = userService.getAllPaged(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Ana Silva");
    }

    @Test
    void getAllLike_deveRetornarResultadoDaBusca() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findAllActiveLike("ana")).thenReturn(List.of(ana));

        List<UserResponse> result = userService.getAllLike("ana");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).email()).isEqualTo("ana@email.com");
    }

    @Test
    void getAllLikePaged_deveRetornarResultadoDaBuscaPaginado() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findAllActiveLike(eq("ana"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ana)));

        Page<UserResponse> result = userService.getAllLikePaged("ana", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Ana Silva");
    }

    @Test
    void getAllQuery_deveIgnorarUsuariosComSoftDelete() {
        User active = buildUser(1L, "Ana Silva", "ana@email.com");
        User deleted = buildUser(2L, "Bia Souza", "bia@email.com");
        deleted.setDeletedAt(LocalDateTime.now());
        when(userRepository.findAll(any(Example.class))).thenReturn(List.of(active, deleted));

        List<UserResponse> result = userService.getAllQuery("Ana", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getAllQueryPaged_deveRetornarPage_quandoNaoEncontraSoftDelete() {
        User active = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findAll(any(Example.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active)));

        Page<UserResponse> result = userService.getAllQueryPaged(null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Ana Silva");
    }

    @Test
    void getAllQueryPaged_deveRetornar404_quandoEncontraUsuarioDeletado() {
        User deleted = buildUser(2L, "Bia Souza", "bia@email.com");
        deleted.setDeletedAt(LocalDateTime.now());
        when(userRepository.findAll(any(Example.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deleted)));

        assertThatThrownBy(() -> userService.getAllQueryPaged(null, null, PageRequest.of(0, 10)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getById_deveRetornarUsuario_quandoEncontrado() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));

        UserResponse result = userService.getById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("ana@email.com");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void getById_deveRetornar404_quandoNaoEncontrado() {
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== RELACIONAIS (with-relations) ==========

    @Test
    void getAllWithRelations_deveMontarColecoesEItemsAtivos() {
        User owner = fixtureOwnerWithRelations();
        when(userRepository.findAllActiveWithRelations()).thenReturn(List.of(owner));

        List<UserWithRelationsResponse> result = userService.getAllWithRelations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).collections()).hasSize(1);
        assertThat(result.get(0).collections().get(0).items()).hasSize(1);
        assertThat(result.get(0).collections().get(0).name()).isEqualTo("Colecao Ativa");
    }

    @Test
    void getByIdWithRelations_deveMontarColecoesEItemsAtivos_quandoEncontrado() {
        User owner = fixtureOwnerWithRelations();
        when(userRepository.findByIdActiveWithRelations(1L)).thenReturn(Optional.of(owner));

        UserWithRelationsResponse result = userService.getByIdWithRelations(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.collections()).hasSize(1);
        assertThat(result.collections().get(0).items()).hasSize(1);
    }

    @Test
    void getByIdWithRelations_deveRetornar404_quandoNaoEncontrado() {
        when(userRepository.findByIdActiveWithRelations(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByIdWithRelations(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== LEFT JOIN (linhas cruas) ==========

    @Test
    void getAllLeftJoin_deveMapearLinhasComNullESoftDelete() {
        User owner = buildUser(1L, "Ana Silva", "ana@email.com");
        CollectionItem activeItem = buildItem(11L, "Item Ativo", false);
        CollectionItem deletedItem = buildItem(12L, "Item Deletado", true);
        Collection active = buildCollection(21L, "Colecao Ativa", owner, false);
        Collection deleted = buildCollection(22L, "Colecao Deletada", owner, true);
        activeItem.setCollection(active);
        deletedItem.setCollection(active);

        List<Object[]> rows = List.of(
                new Object[]{owner, null, null},
                new Object[]{owner, deleted, null},
                new Object[]{owner, active, deletedItem},
                new Object[]{owner, active, activeItem});
        when(userRepository.findAllActiveLeftJoinRows()).thenReturn(rows);

        List<UserLeftJoinRowResponse> result = userService.getAllLeftJoin();

        assertThat(result).hasSize(4);
        assertThat(result.get(0).collection()).isNull();
        assertThat(result.get(0).item()).isNull();
        assertThat(result.get(1).collection()).isNull();
        assertThat(result.get(2).collection()).isNotNull();
        assertThat(result.get(2).item()).isNull();
        assertThat(result.get(3).collection()).isNotNull();
        assertThat(result.get(3).collection().name()).isEqualTo("Colecao Ativa");
        assertThat(result.get(3).item()).isNotNull();
        assertThat(result.get(3).item().name()).isEqualTo("Item Ativo");
    }

    @Test
    void getByIdLeftJoin_deveMapearLinhaSimples_quandoEncontrado() {
        User owner = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findActiveLeftJoinRowsById(1L))
                .thenReturn(List.<Object[]>of(new Object[]{owner, null, null}));

        List<UserLeftJoinRowResponse> result = userService.getByIdLeftJoin(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).user().name()).isEqualTo("Ana Silva");
        assertThat(result.get(0).collection()).isNull();
    }

    @Test
    void getByIdLeftJoin_deveRetornar404_quandoSemLinhas() {
        when(userRepository.findActiveLeftJoinRowsById(99L)).thenReturn(List.<Object[]>of());

        assertThatThrownBy(() -> userService.getByIdLeftJoin(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== SPECIFICATIONS ==========

    @Test
    void getAllWithSpecifications_deveExecutarSpecificationComFetchDuplo() {
        User owner = fixtureOwnerWithRelations();
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(owner));

        List<UserWithRelationsResponse> result = userService.getAllWithSpecifications();

        verify(userRepository).findAll(specCaptor.capture());
        invokeSpecAndVerifyFetch(specCaptor.getValue());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Ana Silva");
    }

    @Test
    void getByIdWithSpecifications_deveExecutarSpecificationPorId_quandoEncontrado() {
        User owner = fixtureOwnerWithRelations();
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(owner));

        UserWithRelationsResponse result = userService.getByIdWithSpecifications(1L);

        verify(userRepository).findAll(specCaptor.capture());
        invokeSpecAndVerifyFetch(specCaptor.getValue());

        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void getByIdWithSpecifications_deveRetornar404_quandoNaoEncontrado() {
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThatThrownBy(() -> userService.getByIdWithSpecifications(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== UPDATE (PUT) ==========

    @Test
    void update_deveAtualizarCompleto_quandoMesmoEmailESenhaInformada() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        stubSave();
        UpdateUserDto dto = new UpdateUserDto("Ana Nova", "ana@email.com", "novaSenha123", "nova desc");

        UserResponse response = userService.update(1L, dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(response.name()).isEqualTo("Ana Nova");
        assertThat(saved.getPassword()).isNotEqualTo("novaSenha123");
        assertThat(saved.getPassword()).startsWith("$2a$");
    }

    @Test
    void update_deveRetornar409_quandoEmailJaExisteEmOutroUsuario() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        when(userRepository.existsByEmailAndIdNot("bia@email.com", 1L)).thenReturn(true);
        UpdateUserDto dto = new UpdateUserDto("Ana Silva", "bia@email.com", null, "dev");

        assertThatThrownBy(() -> userService.update(1L, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void update_devePermitirEmailNovo_quandoLivreESemSenha() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        when(userRepository.existsByEmailAndIdNot("novo@email.com", 1L)).thenReturn(false);
        stubSave();
        UpdateUserDto dto = new UpdateUserDto("Ana Silva", "novo@email.com", null, "dev");

        UserResponse response = userService.update(1L, dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(response.email()).isEqualTo("novo@email.com");
        assertThat(saved.getPassword()).isEqualTo("encoded");
    }

    // ========== UPDATE (PATCH) ==========

    @Test
    void partialUpdate_deveManterTudo_quandoDtoTodoNulo() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        stubSave();
        PatchUserDto dto = new PatchUserDto(null, null, null, null);

        UserResponse response = userService.partialUpdate(1L, dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(response.name()).isEqualTo("Ana Silva");
        assertThat(saved.getEmail()).isEqualTo("ana@email.com");
        assertThat(saved.getPassword()).isEqualTo("encoded");
    }

    @Test
    void partialUpdate_deveAtualizarTodosOsCampos_quandoInformadosComEmailIgual() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        stubSave();
        PatchUserDto dto = new PatchUserDto("Ana P.", "ana@email.com", "senhaForte123", "atualizada");

        UserResponse response = userService.partialUpdate(1L, dto);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(response.name()).isEqualTo("Ana P.");
        assertThat(response.description()).isEqualTo("atualizada");
        assertThat(saved.getPassword()).isNotEqualTo("senhaForte123");
        assertThat(saved.getPassword()).startsWith("$2a$");
    }

    @Test
    void partialUpdate_deveRetornar409_quandoEmailJaExisteEmOutroUsuario() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        when(userRepository.existsByEmailAndIdNot("bia@email.com", 1L)).thenReturn(true);
        PatchUserDto dto = new PatchUserDto(null, "bia@email.com", null, null);

        assertThatThrownBy(() -> userService.partialUpdate(1L, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void partialUpdate_deveAtualizarEmail_quandoLivre() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        when(userRepository.existsByEmailAndIdNot("novo@email.com", 1L)).thenReturn(false);
        stubSave();
        PatchUserDto dto = new PatchUserDto(null, "novo@email.com", null, null);

        UserResponse response = userService.partialUpdate(1L, dto);

        assertThat(response.email()).isEqualTo("novo@email.com");
    }

    // ========== DELETE (soft) ==========

    @Test
    void delete_deveMarcarDeletedAt_quandoEncontrado() {
        User ana = buildUser(1L, "Ana Silva", "ana@email.com");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(ana));
        stubSave();

        userService.delete(1L);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getDeletedAt()).isNotNull();
    }

    // ========== HELPERS ==========

    private void stubSave() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(1L);
            }
            return user;
        });
    }

    private void invokeSpecAndVerifyFetch(Specification spec) {
        doReturn(fetch).when(root).fetch(eq("collections"), eq(JoinType.LEFT));
        doReturn(fetch).when(fetch).fetch(eq("items"), eq(JoinType.LEFT));

        spec.toPredicate(root, query, cb);

        verify(query).distinct(true);
        verify(root).fetch("collections", JoinType.LEFT);
        verify(fetch).fetch("items", JoinType.LEFT);
    }

    private User buildUser(Long id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setDescription("desc");
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        return user;
    }

    private Collection buildCollection(Long id, String name, User owner, boolean isDeleted) {
        Collection collection = new Collection();
        collection.setId(id);
        collection.setName(name);
        collection.setCode("CODE" + id);
        collection.setDescription("desc");
        collection.setOwner(owner);
        collection.setItems(List.of());
        collection.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        collection.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        if (isDeleted) {
            collection.setDeletedAt(LocalDateTime.now());
        }
        return collection;
    }

    private CollectionItem buildItem(Long id, String name, boolean isDeleted) {
        CollectionItem item = new CollectionItem();
        item.setId(id);
        item.setName(name);
        item.setCode("ITEM" + id);
        item.setDescription("desc");
        item.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        if (isDeleted) {
            item.setDeletedAt(LocalDateTime.now());
        }
        return item;
    }

    private User fixtureOwnerWithRelations() {
        User owner = buildUser(1L, "Ana Silva", "ana@email.com");

        CollectionItem activeItem = buildItem(11L, "Item Ativo", false);
        CollectionItem deletedItem = buildItem(12L, "Item Deletado", true);

        Collection active = buildCollection(21L, "Colecao Ativa", owner, false);
        Collection deleted = buildCollection(22L, "Colecao Deletada", owner, true);

        activeItem.setCollection(active);
        deletedItem.setCollection(active);
        active.setItems(List.of(activeItem, deletedItem));
        owner.setCollections(List.of(active, deleted));

        return owner;
    }
}
