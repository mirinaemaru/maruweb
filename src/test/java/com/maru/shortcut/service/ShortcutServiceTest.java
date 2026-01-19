package com.maru.shortcut.service;

import com.maru.shortcut.entity.Shortcut;
import com.maru.shortcut.entity.ShortcutCategory;
import com.maru.shortcut.repository.ShortcutCategoryRepository;
import com.maru.shortcut.repository.ShortcutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortcutService 단위 테스트")
class ShortcutServiceTest {

    @Mock
    private ShortcutRepository shortcutRepository;

    @Mock
    private ShortcutCategoryRepository categoryRepository;

    private ShortcutService shortcutService;

    @BeforeEach
    void setUp() {
        shortcutService = new ShortcutService(shortcutRepository, categoryRepository);
    }

    private ShortcutCategory createCategory(Long id, String name) {
        ShortcutCategory category = new ShortcutCategory();
        category.setId(id);
        category.setName(name);
        category.setDescription("카테고리 설명");
        category.setDisplayOrder(0);
        category.setDeleted("N");
        category.setShortcuts(new ArrayList<>());
        return category;
    }

    private Shortcut createShortcut(Long id, String name, ShortcutCategory category) {
        Shortcut shortcut = new Shortcut();
        shortcut.setId(id);
        shortcut.setName(name);
        shortcut.setUrl("https://example.com");
        shortcut.setDescription("단축키 설명");
        shortcut.setDisplayOrder(0);
        shortcut.setCategory(category);
        shortcut.setDeleted("N");
        return shortcut;
    }

    // ==================== Category Tests ====================

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("모든 카테고리 조회 - 성공")
        void getAllCategories_Success() {
            // given
            ShortcutCategory cat1 = createCategory(1L, "개발");
            ShortcutCategory cat2 = createCategory(2L, "업무");
            when(categoryRepository.findByDeletedOrderByDisplayOrderAsc("N"))
                    .thenReturn(Arrays.asList(cat1, cat2));

            // when
            List<ShortcutCategory> result = shortcutService.getAllCategories();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getAllCategoriesWithShortcuts")
    class GetAllCategoriesWithShortcutsTests {

        @Test
        @DisplayName("모든 카테고리와 단축키 조회 - 삭제된 단축키 제외")
        void getAllCategoriesWithShortcuts_FilterDeleted() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            Shortcut shortcut1 = createShortcut(1L, "GitHub", cat);
            Shortcut shortcut2 = createShortcut(2L, "삭제된 단축키", cat);
            shortcut2.setDeleted("Y");
            cat.setShortcuts(new ArrayList<>(Arrays.asList(shortcut1, shortcut2)));

            when(categoryRepository.findByDeletedOrderByDisplayOrderAsc("N"))
                    .thenReturn(Arrays.asList(cat));

            // when
            List<ShortcutCategory> result = shortcutService.getAllCategoriesWithShortcuts();

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getShortcuts()).hasSize(1);
            assertThat(result.get(0).getShortcuts().get(0).getName()).isEqualTo("GitHub");
        }
    }

    @Nested
    @DisplayName("getCategoryById")
    class GetCategoryByIdTests {

        @Test
        @DisplayName("ID로 카테고리 조회 - 존재하는 경우")
        void getCategoryById_Found() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            when(categoryRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(cat));

            // when
            Optional<ShortcutCategory> result = shortcutService.getCategoryById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("개발");
        }

        @Test
        @DisplayName("ID로 카테고리 조회 - 존재하지 않음")
        void getCategoryById_NotFound() {
            // given
            when(categoryRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when
            Optional<ShortcutCategory> result = shortcutService.getCategoryById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("카테고리 생성 - 성공")
        void createCategory_Success() {
            // given
            ShortcutCategory newCategory = new ShortcutCategory();
            newCategory.setName("새 카테고리");

            when(categoryRepository.save(any(ShortcutCategory.class))).thenAnswer(inv -> {
                ShortcutCategory saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            ShortcutCategory result = shortcutService.createCategory(newCategory);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDisplayOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("카테고리 생성 - displayOrder가 있는 경우")
        void createCategory_WithDisplayOrder() {
            // given
            ShortcutCategory newCategory = new ShortcutCategory();
            newCategory.setName("새 카테고리");
            newCategory.setDisplayOrder(5);

            when(categoryRepository.save(any(ShortcutCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            ShortcutCategory result = shortcutService.createCategory(newCategory);

            // then
            assertThat(result.getDisplayOrder()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("카테고리 수정 - 성공")
        void updateCategory_Success() {
            // given
            ShortcutCategory existing = createCategory(1L, "기존 카테고리");
            ShortcutCategory updated = new ShortcutCategory();
            updated.setName("수정된 카테고리");
            updated.setDescription("수정된 설명");
            updated.setDisplayOrder(10);

            when(categoryRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(existing));
            when(categoryRepository.save(any(ShortcutCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            ShortcutCategory result = shortcutService.updateCategory(1L, updated);

            // then
            assertThat(result.getName()).isEqualTo("수정된 카테고리");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getDisplayOrder()).isEqualTo(10);
        }

        @Test
        @DisplayName("카테고리 수정 - 존재하지 않음")
        void updateCategory_NotFound() {
            // given
            ShortcutCategory updated = new ShortcutCategory();
            updated.setName("수정된 카테고리");

            when(categoryRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> shortcutService.updateCategory(999L, updated))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Category not found: 999");
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("카테고리 삭제 - 단축키도 함께 삭제")
        void deleteCategory_WithShortcuts() {
            // given
            ShortcutCategory cat = createCategory(1L, "카테고리");
            Shortcut shortcut1 = createShortcut(1L, "단축키1", cat);
            Shortcut shortcut2 = createShortcut(2L, "단축키2", cat);
            cat.setShortcuts(new ArrayList<>(Arrays.asList(shortcut1, shortcut2)));

            when(categoryRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(cat));
            when(categoryRepository.save(any(ShortcutCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            shortcutService.deleteCategory(1L);

            // then
            ArgumentCaptor<ShortcutCategory> captor = ArgumentCaptor.forClass(ShortcutCategory.class);
            verify(categoryRepository).save(captor.capture());

            ShortcutCategory deletedCat = captor.getValue();
            assertThat(deletedCat.getDeleted()).isEqualTo("Y");
            assertThat(deletedCat.getShortcuts()).allMatch(s -> "Y".equals(s.getDeleted()));
        }

        @Test
        @DisplayName("카테고리 삭제 - 존재하지 않음")
        void deleteCategory_NotFound() {
            // given
            when(categoryRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> shortcutService.deleteCategory(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Category not found: 999");
        }
    }

    // ==================== Shortcut Tests ====================

    @Nested
    @DisplayName("getAllShortcuts")
    class GetAllShortcutsTests {

        @Test
        @DisplayName("모든 단축키 조회 - 성공")
        void getAllShortcuts_Success() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            Shortcut s1 = createShortcut(1L, "GitHub", cat);
            Shortcut s2 = createShortcut(2L, "GitLab", cat);
            when(shortcutRepository.findByDeletedOrderByDisplayOrderAsc("N"))
                    .thenReturn(Arrays.asList(s1, s2));

            // when
            List<Shortcut> result = shortcutService.getAllShortcuts();

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getShortcutsByCategory")
    class GetShortcutsByCategoryTests {

        @Test
        @DisplayName("카테고리별 단축키 조회 - 성공")
        void getShortcutsByCategory_Success() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            Shortcut s1 = createShortcut(1L, "GitHub", cat);
            when(shortcutRepository.findByCategoryAndDeletedOrderByDisplayOrderAsc(cat, "N"))
                    .thenReturn(Arrays.asList(s1));

            // when
            List<Shortcut> result = shortcutService.getShortcutsByCategory(cat);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("GitHub");
        }
    }

    @Nested
    @DisplayName("getShortcutById")
    class GetShortcutByIdTests {

        @Test
        @DisplayName("ID로 단축키 조회 - 존재하는 경우")
        void getShortcutById_Found() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            Shortcut shortcut = createShortcut(1L, "GitHub", cat);
            when(shortcutRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(shortcut));

            // when
            Optional<Shortcut> result = shortcutService.getShortcutById(1L);

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("GitHub");
        }

        @Test
        @DisplayName("ID로 단축키 조회 - 존재하지 않음")
        void getShortcutById_NotFound() {
            // given
            when(shortcutRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when
            Optional<Shortcut> result = shortcutService.getShortcutById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createShortcut")
    class CreateShortcutTests {

        @Test
        @DisplayName("단축키 생성 - 성공")
        void createShortcut_Success() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            Shortcut newShortcut = new Shortcut();
            newShortcut.setName("새 단축키");
            newShortcut.setUrl("https://example.com");
            newShortcut.setCategory(cat);

            when(shortcutRepository.save(any(Shortcut.class))).thenAnswer(inv -> {
                Shortcut saved = inv.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // when
            Shortcut result = shortcutService.createShortcut(newShortcut);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDisplayOrder()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("updateShortcut")
    class UpdateShortcutTests {

        @Test
        @DisplayName("단축키 수정 - 성공")
        void updateShortcut_Success() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            ShortcutCategory newCat = createCategory(2L, "업무");
            Shortcut existing = createShortcut(1L, "GitHub", cat);

            Shortcut updated = new Shortcut();
            updated.setName("수정된 단축키");
            updated.setUrl("https://modified.com");
            updated.setDescription("수정된 설명");
            updated.setDisplayOrder(5);
            updated.setCategory(newCat);

            when(shortcutRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(existing));
            when(shortcutRepository.save(any(Shortcut.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            Shortcut result = shortcutService.updateShortcut(1L, updated);

            // then
            assertThat(result.getName()).isEqualTo("수정된 단축키");
            assertThat(result.getUrl()).isEqualTo("https://modified.com");
            assertThat(result.getDescription()).isEqualTo("수정된 설명");
            assertThat(result.getDisplayOrder()).isEqualTo(5);
            assertThat(result.getCategory().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("단축키 수정 - 존재하지 않음")
        void updateShortcut_NotFound() {
            // given
            Shortcut updated = new Shortcut();
            updated.setName("수정된 단축키");

            when(shortcutRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> shortcutService.updateShortcut(999L, updated))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Shortcut not found: 999");
        }
    }

    @Nested
    @DisplayName("deleteShortcut")
    class DeleteShortcutTests {

        @Test
        @DisplayName("단축키 삭제 - 성공")
        void deleteShortcut_Success() {
            // given
            ShortcutCategory cat = createCategory(1L, "개발");
            Shortcut shortcut = createShortcut(1L, "GitHub", cat);
            when(shortcutRepository.findByIdAndDeleted(1L, "N")).thenReturn(Optional.of(shortcut));
            when(shortcutRepository.save(any(Shortcut.class))).thenAnswer(inv -> inv.getArgument(0));

            // when
            shortcutService.deleteShortcut(1L);

            // then
            ArgumentCaptor<Shortcut> captor = ArgumentCaptor.forClass(Shortcut.class);
            verify(shortcutRepository).save(captor.capture());
            assertThat(captor.getValue().getDeleted()).isEqualTo("Y");
        }

        @Test
        @DisplayName("단축키 삭제 - 존재하지 않음")
        void deleteShortcut_NotFound() {
            // given
            when(shortcutRepository.findByIdAndDeleted(999L, "N")).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> shortcutService.deleteShortcut(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Shortcut not found: 999");
        }
    }
}
