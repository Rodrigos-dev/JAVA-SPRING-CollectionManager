package com.ars.collectionmanager.modules.collectionitem;

import com.ars.collectionmanager.modules.collection.entities.Collection;
import com.ars.collectionmanager.modules.collectionitem.dtos.CollectionItemResponse;
import com.ars.collectionmanager.modules.collectionitem.dtos.CreateCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.dtos.PatchCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.dtos.UpdateCollectionItemDto;
import com.ars.collectionmanager.modules.collectionitem.entities.CollectionItem;
import com.ars.collectionmanager.modules.user.UserRepository;
import com.ars.collectionmanager.modules.user.entities.User;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionItemServiceTest {

    private static final String OWNER_EMAIL = "ana@email.com";

    @Mock
    private CollectionItemRepository collectionItemRepository;

    @Mock
    private com.ars.collectionmanager.modules.collection.CollectionRepository collectionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CollectionItemService collectionItemService;

    @Captor
    private ArgumentCaptor<CollectionItem> itemCaptor;

    private User owner;
    private Collection collection;

    @BeforeEach
    void setUp() {
        owner = buildUser(1L, "Ana Silva", OWNER_EMAIL);
        collection = buildCollection(21L, "Favoritos", owner);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(OWNER_EMAIL, "n/a"));
        lenient().when(userRepository.findByEmailAndDeletedAtIsNull(OWNER_EMAIL))
                .thenReturn(Optional.of(owner));
        lenient().when(collectionRepository.findByIdAndDeletedAtIsNull(21L))
                .thenReturn(Optional.of(collection));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========== CREATE ==========

    @Test
    void create_deveCriarItem_quandoCodigoLivreNaColecao() {
        CreateCollectionItemDto dto = new CreateCollectionItemDto("Livro", "LIV01", "desc", 21L);
        when(collectionItemRepository.existsByCollectionIdAndCode(21L, "LIV01")).thenReturn(false);
        stubSave();

        CollectionItemResponse response = collectionItemService.create(dto);

        verify(collectionItemRepository).save(itemCaptor.capture());
        CollectionItem saved = itemCaptor.getValue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Livro");
        assertThat(response.code()).isEqualTo("LIV01");
        assertThat(response.collectionId()).isEqualTo(21L);
        assertThat(response.collectionName()).isEqualTo("Favoritos");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("Ana Silva");
        assertThat(saved.getCollection()).isEqualTo(collection);
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void create_deveRetornar409_quandoCodigoJaExisteNaColecao() {
        CreateCollectionItemDto dto = new CreateCollectionItemDto("Livro", "LIV01", "desc", 21L);
        when(collectionItemRepository.existsByCollectionIdAndCode(21L, "LIV01")).thenReturn(true);

        assertThatThrownBy(() -> collectionItemService.create(dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void create_deveRetornar404_quandoColecaoNaoExiste() {
        when(collectionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        CreateCollectionItemDto dto = new CreateCollectionItemDto("Livro", "LIV01", "desc", 99L);

        assertThatThrownBy(() -> collectionItemService.create(dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void create_deveRetornar404_quandoColecaoEDeOutroDono() {
        User stranger = buildUser(2L, "Bia", "bia@email.com");
        Collection foreign = buildCollection(22L, "Outra", stranger);
        when(collectionRepository.findByIdAndDeletedAtIsNull(22L)).thenReturn(Optional.of(foreign));
        CreateCollectionItemDto dto = new CreateCollectionItemDto("Livro", "LIV01", "desc", 22L);

        assertThatThrownBy(() -> collectionItemService.create(dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== READ ==========

    @Test
    void getAll_deveListarPorColecao_quandoCollectionIdInformado() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findAllActiveByCollectionId(21L)).thenReturn(List.of(item));

        List<CollectionItemResponse> result = collectionItemService.getAll(21L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Livro");
        assertThat(result.get(0).collectionId()).isEqualTo(21L);
        verify(collectionItemRepository, never()).findAllActiveByOwnerId(any());
    }

    @Test
    void getAll_deveListarPorOwner_quandoCollectionIdNulo() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findAllActiveByOwnerId(1L)).thenReturn(List.of(item));

        List<CollectionItemResponse> result = collectionItemService.getAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Livro");
        verify(collectionItemRepository, never()).findAllActiveByCollectionId(any());
    }

    @Test
    void getAll_deveRetornar404_quandoColecaoNaoEDeOutroDono() {
        User stranger = buildUser(2L, "Bia", "bia@email.com");
        Collection foreign = buildCollection(22L, "Outra", stranger);
        when(collectionRepository.findByIdAndDeletedAtIsNull(22L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> collectionItemService.getAll(22L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getAllPaged_deveListarPorColecao_quandoCollectionIdInformado() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findAllActiveByCollectionId(eq(21L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<CollectionItemResponse> result = collectionItemService.getAllPaged(21L, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Livro");
        verify(collectionItemRepository, never()).findAllActiveByOwnerId(any(), any(Pageable.class));
    }

    @Test
    void getAllPaged_deveListarPorOwner_quandoCollectionIdNulo() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findAllActiveByOwnerId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<CollectionItemResponse> result = collectionItemService.getAllPaged(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Livro");
        verify(collectionItemRepository, never()).findAllActiveByCollectionId(any(), any(Pageable.class));
    }

    @Test
    void getAllPaged_deveRetornar404_quandoColecaoNaoExiste() {
        when(collectionRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionItemService.getAllPaged(99L, PageRequest.of(0, 10)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getAllLike_deveRetornarResultadoDaBusca() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findAllActiveLikeByOwnerId(1L, "liv")).thenReturn(List.of(item));

        List<CollectionItemResponse> result = collectionItemService.getAllLike("liv");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("LIV01");
    }

    @Test
    void getAllLikePaged_deveRetornarPageDaBusca() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findAllActiveLikeByOwnerId(eq(1L), eq("liv"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<CollectionItemResponse> result = collectionItemService.getAllLikePaged("liv", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Livro");
    }

    @Test
    void getById_deveRetornarItem_quandoEncontrado() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));

        CollectionItemResponse result = collectionItemService.getById(11L);

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.collectionId()).isEqualTo(21L);
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void getById_deveRetornar404_quandoNaoEncontrado() {
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionItemService.getById(99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getById_deveRetornar404_quandoColecaoEDeOutroDono() {
        User stranger = buildUser(2L, "Bia", "bia@email.com");
        Collection foreign = buildCollection(22L, "Outra", stranger);
        CollectionItem item = buildItem(11L, "Livro", "LIV01", foreign, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> collectionItemService.getById(11L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ========== UPDATE (PUT) ==========

    @Test
    void update_deveAtualizarCompleto_quandoMesmoCodigo() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        stubSave();
        UpdateCollectionItemDto dto = new UpdateCollectionItemDto("Livro Novo", "LIV01", "nova desc");

        CollectionItemResponse response = collectionItemService.update(11L, dto);

        verify(collectionItemRepository).save(itemCaptor.capture());
        CollectionItem saved = itemCaptor.getValue();
        assertThat(response.name()).isEqualTo("Livro Novo");
        assertThat(saved.getCode()).isEqualTo("LIV01");
        assertThat(saved.getDescription()).isEqualTo("nova desc");
        verify(collectionItemRepository, never()).existsByCollectionIdAndCodeAndIdNot(any(), anyString(), any());
    }

    @Test
    void update_deveAtualizarCodigo_quandoLivre() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        when(collectionItemRepository.existsByCollectionIdAndCodeAndIdNot(21L, "NOVO01", 11L)).thenReturn(false);
        stubSave();
        UpdateCollectionItemDto dto = new UpdateCollectionItemDto("Livro", "NOVO01", "desc");

        CollectionItemResponse response = collectionItemService.update(11L, dto);

        assertThat(response.code()).isEqualTo("NOVO01");
        verify(collectionItemRepository).save(any(CollectionItem.class));
    }

    @Test
    void update_deveRetornar409_quandoCodigoJaExisteNaColecao() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        when(collectionItemRepository.existsByCollectionIdAndCodeAndIdNot(21L, "OUTRO01", 11L)).thenReturn(true);
        UpdateCollectionItemDto dto = new UpdateCollectionItemDto("Livro", "OUTRO01", "desc");

        assertThatThrownBy(() -> collectionItemService.update(11L, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ========== UPDATE (PATCH) ==========

    @Test
    void partialUpdate_deveManterTudo_quandoDtoTodoNulo() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        stubSave();
        PatchCollectionItemDto dto = new PatchCollectionItemDto(null, null, null);

        CollectionItemResponse response = collectionItemService.partialUpdate(11L, dto);

        verify(collectionItemRepository).save(itemCaptor.capture());
        CollectionItem saved = itemCaptor.getValue();
        assertThat(response.name()).isEqualTo("Livro");
        assertThat(saved.getCode()).isEqualTo("LIV01");
        assertThat(saved.getDescription()).isEqualTo("desc");
    }

    @Test
    void partialUpdate_deveAtualizarTodosOsCampos_quandoInformados() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        when(collectionItemRepository.existsByCollectionIdAndCodeAndIdNot(21L, "FIC01", 11L)).thenReturn(false);
        stubSave();
        PatchCollectionItemDto dto = new PatchCollectionItemDto("Ficcao", "FIC01", "atualizada");

        CollectionItemResponse response = collectionItemService.partialUpdate(11L, dto);

        verify(collectionItemRepository).save(itemCaptor.capture());
        CollectionItem saved = itemCaptor.getValue();
        assertThat(response.name()).isEqualTo("Ficcao");
        assertThat(saved.getCode()).isEqualTo("FIC01");
        assertThat(saved.getDescription()).isEqualTo("atualizada");
    }

    @Test
    void partialUpdate_deveAtualizarName_quandoCodeNulo() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        stubSave();
        PatchCollectionItemDto dto = new PatchCollectionItemDto("So Nome", null, null);

        CollectionItemResponse response = collectionItemService.partialUpdate(11L, dto);

        assertThat(response.name()).isEqualTo("So Nome");
        assertThat(response.code()).isEqualTo("LIV01");
    }

    @Test
    void partialUpdate_devePularChecagemDeCodigo_quandoCodigoIgual() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        stubSave();
        PatchCollectionItemDto dto = new PatchCollectionItemDto("Livro P.", "LIV01", null);

        CollectionItemResponse response = collectionItemService.partialUpdate(11L, dto);

        assertThat(response.name()).isEqualTo("Livro P.");
        assertThat(response.code()).isEqualTo("LIV01");
        verify(collectionItemRepository, never()).existsByCollectionIdAndCodeAndIdNot(any(), anyString(), any());
    }

    @Test
    void partialUpdate_deveRetornar409_quandoCodigoJaExisteNaColecao() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        when(collectionItemRepository.existsByCollectionIdAndCodeAndIdNot(21L, "OUTRO01", 11L)).thenReturn(true);
        PatchCollectionItemDto dto = new PatchCollectionItemDto(null, "OUTRO01", null);

        assertThatThrownBy(() -> collectionItemService.partialUpdate(11L, dto))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    // ========== DELETE (soft) ==========

    @Test
    void delete_deveMarcarDeletedAt_quandoEncontrado() {
        CollectionItem item = buildItem(11L, "Livro", "LIV01", collection, false);
        when(collectionItemRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(Optional.of(item));
        stubSave();

        collectionItemService.delete(11L);

        verify(collectionItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getDeletedAt()).isNotNull();
    }

    // ========== HELPERS: FORBIDDEN ==========

    @Test
    void getAll_deveRetornar403_quandoSemAutenticacao() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> collectionItemService.getAll(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAll_deveRetornar403_quandoNomeAutenticacaoNulo() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> collectionItemService.getAll(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void getAll_deveRetornar403_quandoUsuarioNaoEncontrado() {
        when(userRepository.findByEmailAndDeletedAtIsNull(OWNER_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionItemService.getAll(null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    // ========== HELPERS ==========

    private void stubSave() {
        when(collectionItemRepository.save(any(CollectionItem.class))).thenAnswer(invocation -> {
            CollectionItem item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(1L);
            }
            return item;
        });
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

    private Collection buildCollection(Long id, String name, User owner) {
        Collection col = new Collection();
        col.setId(id);
        col.setName(name);
        col.setCode("CODE" + id);
        col.setDescription("desc");
        col.setOwner(owner);
        col.setItems(List.of());
        col.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        col.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        return col;
    }

    private CollectionItem buildItem(Long id, String name, String code, Collection col, boolean isDeleted) {
        CollectionItem item = new CollectionItem();
        item.setId(id);
        item.setName(name);
        item.setCode(code);
        item.setDescription("desc");
        item.setCollection(col);
        item.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
        item.setUpdatedAt(LocalDateTime.of(2026, 1, 2, 10, 0));
        if (isDeleted) {
            item.setDeletedAt(LocalDateTime.now());
        }
        return item;
    }
}
