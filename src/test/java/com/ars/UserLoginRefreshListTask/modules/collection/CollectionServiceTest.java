package com.ars.UserLoginRefreshListTask.modules.collection;

import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CollectionLeftJoinRowResponse;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CollectionResponse;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CollectionWithItemsResponse;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.CreateCollectionDto;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.PatchCollectionDto;
import com.ars.UserLoginRefreshListTask.modules.collection.dtos.UpdateCollectionDto;
import com.ars.UserLoginRefreshListTask.modules.collection.entities.Collection;
import com.ars.UserLoginRefreshListTask.modules.collectionitem.entities.CollectionItem;
import com.ars.UserLoginRefreshListTask.modules.user.UserRepository;
import com.ars.UserLoginRefreshListTask.modules.user.entities.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    private static final String OWNER_EMAIL = "ana@email.com";

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CollectionService collectionService;

    @Captor
    private ArgumentCaptor<Collection> collectionCaptor;

    @Captor
    private ArgumentCaptor<Specification> specCaptor;

    @Mock
    private Root<Collection> root;

    @Mock
    private CriteriaQuery query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Fetch fetch;

    @Mock
    private Path path;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = buildUser(1L, "Ana Silva", OWNER_EMAIL);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_EMAIL, "n/a"));
        lenient().when(userRepository.findByEmailAndDeletedAtIsNull(OWNER_EMAIL))
                .thenReturn(Optional.of(owner));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== CREATE ==========

    @Test
    void create_deveCriarColecao_quandoCodigoNaoExiste() {
        CreateCollectionDto dto = new CreateCollectionDto("Favoritos", "FAV01", "desc");

        when(collectionRepository.existsByCode("FAV01")).thenReturn(false);
        stubSave();

        CollectionResponse response = collectionService.create(dto);

        verify(collectionRepository).save(collectionCaptor.capture());
        Collection saved = collectionCaptor.getValue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Favoritos");
        assertThat(response.code()).isEqualTo("FAV01");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("Ana Silva");
        assertThat(saved.getOwner()).isEqualTo(owner);
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void create_deveRetornar409_quandoCodigoJaExiste() {
        CreateCollectionDto dto = new CreateCollectionDto("Favoritos", "FAV01", "desc");

        when(collectionRepository.existsByCode("FAV01")).thenReturn(true);

        assertThatThrownBy(() -> collectionService.create(dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ========== READ ==========

    @Test
    void getAll_deveListarColecoesDoUsuario() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        Collection leitura = buildCollection(22L, "Leitura", owner, false);
        when(collectionRepository.findAllActiveByOwnerId(1L)).thenReturn(List.of(fav, leitura));

        List<CollectionResponse> result = collectionService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Favoritos");
        assertThat(result.get(1).code()).isEqualTo("CODE22");
    }

    @Test
    void getAllPaged_deveRetornarPageMapeada() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findAllActiveByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fav)));

        Page<CollectionResponse> result = collectionService.getAllPaged(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Favoritos");
    }

    @Test
    void getAllLike_deveRetornarResultadoDaBusca() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findAllActiveLikeByOwnerId(1L, "fav")).thenReturn(List.of(fav));

        List<CollectionResponse> result = collectionService.getAllLike("fav");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("CODE21");
    }

    @Test
    void getAllLikePaged_deveRetornarPageDaBusca() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findAllActiveLikeByOwnerId(eq(1L), eq("fav"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fav)));

        Page<CollectionResponse> result = collectionService.getAllLikePaged("fav", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Favoritos");
    }

    @Test
    void getById_deveRetornarColecao_quandoEncontrada() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));

        CollectionResponse result = collectionService.getById(21L);

        assertThat(result.id()).isEqualTo(21L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void getById_deveRetornar404_quandoNaoEncontrada() {
        when(collectionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getById(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getById_deveRetornar404_quandoNaoEDono() {
        User stranger = buildUser(2L, "Bia", "bia@email.com");
        Collection foreign = buildCollection(21L, "Favoritos", stranger, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> collectionService.getById(21L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== RELACIONAIS (collection -> items) ==========

    @Test
    void getAllWithRelations_deveMontarItemsAtivosEIgnorarDeletados() {
        Collection fav = fixtureCollectionWithItems();
        when(collectionRepository.findAllActiveWithRelationsByOwnerId(1L)).thenReturn(List.of(fav));

        List<CollectionWithItemsResponse> result = collectionService.getAllWithRelations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).items()).hasSize(1);
        assertThat(result.get(0).items().get(0).name()).isEqualTo("Item Ativo");
        assertThat(result.get(0).items().get(0).collectionId()).isEqualTo(21L);
    }

    @Test
    void getByIdWithRelations_deveMontarItems_quandoEncontrada() {
        Collection fav = fixtureCollectionWithItems();
        when(collectionRepository.findByIdActiveWithRelations(21L)).thenReturn(Optional.of(fav));

        CollectionWithItemsResponse result = collectionService.getByIdWithRelations(21L);

        assertThat(result.id()).isEqualTo(21L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).userId()).isEqualTo(1L);
    }

    @Test
    void getByIdWithRelations_deveRetornar404_quandoNaoEncontrada() {
        when(collectionRepository.findByIdActiveWithRelations(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getByIdWithRelations(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getByIdWithRelations_deveRetornar404_quandoNaoEDono() {
        User stranger = buildUser(2L, "Bia", "bia@email.com");
        Collection foreign = fixtureCollectionWithItems();
        foreign.setOwner(stranger);
        when(collectionRepository.findByIdActiveWithRelations(21L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> collectionService.getByIdWithRelations(21L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== LEFT JOIN (linhas cruas) ==========

    @Test
    void getAllLeftJoin_deveMapearLinhasComNullESoftDelete() {
        Collection fav = fixtureCollectionWithItems();
        CollectionItem activeItem = fav.getItems().get(0);
        CollectionItem deletedItem = fav.getItems().get(1);

        List<Object[]> rows = List.of(
                new Object[]{fav, null},
                new Object[]{fav, deletedItem},
                new Object[]{fav, activeItem});
        when(collectionRepository.findAllActiveLeftJoinRowsByOwnerId(1L)).thenReturn(rows);

        List<CollectionLeftJoinRowResponse> result = collectionService.getAllLeftJoin();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).item()).isNull();
        assertThat(result.get(1).item()).isNull();
        assertThat(result.get(2).item()).isNotNull();
        assertThat(result.get(2).item().name()).isEqualTo("Item Ativo");
        assertThat(result.get(2).collection().name()).isEqualTo("Favoritos");
    }

    @Test
    void getByIdLeftJoin_deveMapearLinhas_quandoEncontrada() {
        Collection fav = fixtureCollectionWithItems();
        CollectionItem activeItem = fav.getItems().get(0);
        when(collectionRepository.findActiveLeftJoinRowsById(21L))
                .thenReturn(List.<Object[]>of(new Object[]{fav, activeItem}));

        List<CollectionLeftJoinRowResponse> result = collectionService.getByIdLeftJoin(21L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).collection().id()).isEqualTo(21L);
        assertThat(result.get(0).item().name()).isEqualTo("Item Ativo");
    }

    @Test
    void getByIdLeftJoin_deveRetornar404_quandoSemLinhas() {
        when(collectionRepository.findActiveLeftJoinRowsById(99L)).thenReturn(List.<Object[]>of());

        assertThatThrownBy(() -> collectionService.getByIdLeftJoin(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getByIdLeftJoin_deveRetornar404_quandoNaoEDono() {
        User stranger = buildUser(2L, "Bia", "bia@email.com");
        Collection foreign = buildCollection(21L, "Favoritos", stranger, false);
        when(collectionRepository.findActiveLeftJoinRowsById(21L))
                .thenReturn(List.<Object[]>of(new Object[]{foreign, null}));

        assertThatThrownBy(() -> collectionService.getByIdLeftJoin(21L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== SPECIFICATIONS ==========

    @Test
    void getAllWithSpecifications_deveExecutarSpecificationComFetch() {
        Collection fav = fixtureCollectionWithItems();
        when(collectionRepository.findAll(any(Specification.class))).thenReturn(List.of(fav));

        List<CollectionWithItemsResponse> result = collectionService.getAllWithSpecifications();

        verify(collectionRepository).findAll(specCaptor.capture());
        invokeSpecAndVerifyFetch(specCaptor.getValue());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Favoritos");
    }

    @Test
    void getByIdWithSpecifications_deveExecutarSpecificationPorId_quandoEncontrada() {
        Collection fav = fixtureCollectionWithItems();
        when(collectionRepository.findAll(any(Specification.class))).thenReturn(List.of(fav));

        CollectionWithItemsResponse result = collectionService.getByIdWithSpecifications(21L);

        verify(collectionRepository).findAll(specCaptor.capture());
        invokeSpecAndVerifyFetch(specCaptor.getValue());

        assertThat(result.id()).isEqualTo(21L);
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void getByIdWithSpecifications_deveRetornar404_quandoNaoEncontrada() {
        when(collectionRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThatThrownBy(() -> collectionService.getByIdWithSpecifications(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== UPDATE (PUT) ==========

    @Test
    void update_deveAtualizarCompleto_quandoMesmoCodigo() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        stubSave();
        UpdateCollectionDto dto = new UpdateCollectionDto("Favoritos Novo", "CODE21", "nova desc");

        CollectionResponse response = collectionService.update(21L, dto);

        verify(collectionRepository).save(collectionCaptor.capture());
        Collection saved = collectionCaptor.getValue();
        assertThat(response.name()).isEqualTo("Favoritos Novo");
        assertThat(saved.getCode()).isEqualTo("CODE21");
        assertThat(saved.getDescription()).isEqualTo("nova desc");
        verify(collectionRepository, never()).existsByCodeAndIdNot(anyString(), any());
    }

    @Test
    void update_deveAtualizarCodigo_quandoLivre() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        when(collectionRepository.existsByCodeAndIdNot("NOVO01", 21L)).thenReturn(false);
        stubSave();
        UpdateCollectionDto dto = new UpdateCollectionDto("Favoritos", "NOVO01", "desc");

        CollectionResponse response = collectionService.update(21L, dto);

        assertThat(response.code()).isEqualTo("NOVO01");
        verify(collectionRepository).save(any(Collection.class));
    }

    @Test
    void update_deveRetornar409_quandoCodigoJaExisteEmOutraColecao() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        when(collectionRepository.existsByCodeAndIdNot("LEIT01", 21L)).thenReturn(true);
        UpdateCollectionDto dto = new UpdateCollectionDto("Favoritos", "LEIT01", "desc");

        assertThatThrownBy(() -> collectionService.update(21L, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ========== UPDATE (PATCH) ==========

    @Test
    void partialUpdate_deveManterTudo_quandoDtoTodoNulo() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        stubSave();
        PatchCollectionDto dto = new PatchCollectionDto(null, null, null);

        CollectionResponse response = collectionService.partialUpdate(21L, dto);

        verify(collectionRepository).save(collectionCaptor.capture());
        Collection saved = collectionCaptor.getValue();
        assertThat(response.name()).isEqualTo("Favoritos");
        assertThat(saved.getCode()).isEqualTo("CODE21");
        assertThat(saved.getDescription()).isEqualTo("desc");
    }

    @Test
    void partialUpdate_deveAtualizarTodosOsCampos_quandoInformados() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        stubSave();
        PatchCollectionDto dto = new PatchCollectionDto("Fav P.", "FAV01", "atualizada");

        CollectionResponse response = collectionService.partialUpdate(21L, dto);

        verify(collectionRepository).save(collectionCaptor.capture());
        Collection saved = collectionCaptor.getValue();
        assertThat(response.name()).isEqualTo("Fav P.");
        assertThat(saved.getCode()).isEqualTo("FAV01");
        assertThat(saved.getDescription()).isEqualTo("atualizada");
    }

    @Test
    void partialUpdate_deveAtualizarName_quandoCodeNulo() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        stubSave();
        PatchCollectionDto dto = new PatchCollectionDto("So Nome", null, null);

        CollectionResponse response = collectionService.partialUpdate(21L, dto);

        assertThat(response.name()).isEqualTo("So Nome");
        assertThat(response.code()).isEqualTo("CODE21");
    }

    @Test
    void partialUpdate_devePularChecagemDeCodigo_quandoCodigoIgual() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        stubSave();
        PatchCollectionDto dto = new PatchCollectionDto("Fav P.", "CODE21", null);

        CollectionResponse response = collectionService.partialUpdate(21L, dto);

        assertThat(response.name()).isEqualTo("Fav P.");
        assertThat(response.code()).isEqualTo("CODE21");
        verify(collectionRepository, never()).existsByCodeAndIdNot(anyString(), any());
    }

    @Test
    void partialUpdate_deveRetornar409_quandoCodigoJaExisteEmOutraColecao() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        when(collectionRepository.existsByCodeAndIdNot("LEIT01", 21L)).thenReturn(true);
        PatchCollectionDto dto = new PatchCollectionDto(null, "LEIT01", null);

        assertThatThrownBy(() -> collectionService.partialUpdate(21L, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ========== DELETE (soft) ==========

    @Test
    void delete_deveMarcarDeletedAt_quandoEncontrada() {
        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        when(collectionRepository.findByIdAndDeletedAtIsNull(21L)).thenReturn(Optional.of(fav));
        stubSave();

        collectionService.delete(21L);

        verify(collectionRepository).save(collectionCaptor.capture());
        assertThat(collectionCaptor.getValue().getDeletedAt()).isNotNull();
    }

    // ========== HELPERS: FORBIDDEN ==========

    @Test
    void getAll_deveRetornar403_quandoSemAutenticacao() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> collectionService.getAll())
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAll_deveRetornar403_quandoNomeAutenticacaoNulo() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> collectionService.getAll())
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAll_deveRetornar403_quandoUsuarioNaoEncontrado() {
        when(userRepository.findByEmailAndDeletedAtIsNull(OWNER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getAll())
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ========== HELPERS ==========

    private void stubSave() {
        when(collectionRepository.save(any(Collection.class))).thenAnswer(invocation -> {
            Collection collection = invocation.getArgument(0);
            if (collection.getId() == null) {
                collection.setId(1L);
            }
            return collection;
        });
    }

    private void invokeSpecAndVerifyFetch(Specification spec) {
        doReturn(fetch).when(root).fetch(eq("items"), eq(JoinType.LEFT));
        doReturn(path).when(root).get(anyString());
        doReturn(path).when(path).get(anyString());

        spec.toPredicate(root, query, cb);

        verify(query).distinct(true);
        verify(root).fetch("items", JoinType.LEFT);
        verify(root).get("owner");
        verify(path).get("id");
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

    private Collection fixtureCollectionWithItems() {
        CollectionItem activeItem = buildItem(11L, "Item Ativo", false);
        CollectionItem deletedItem = buildItem(12L, "Item Deletado", true);

        Collection fav = buildCollection(21L, "Favoritos", owner, false);
        activeItem.setCollection(fav);
        deletedItem.setCollection(fav);
        fav.setItems(List.of(activeItem, deletedItem));

        return fav;
    }
}
