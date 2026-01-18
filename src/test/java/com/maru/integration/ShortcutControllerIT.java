package com.maru.integration;

import com.maru.todo.TodoApplication;
import com.maru.shortcut.entity.Shortcut;
import com.maru.shortcut.entity.ShortcutCategory;
import com.maru.shortcut.repository.ShortcutCategoryRepository;
import com.maru.shortcut.repository.ShortcutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {TodoApplication.class, TestConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ShortcutController 통합테스트")
class ShortcutControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortcutCategoryRepository categoryRepository;

    @Autowired
    private ShortcutRepository shortcutRepository;

    @BeforeEach
    void setUp() {
        shortcutRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    // ========== Category Tests ==========

    @Test
    @DisplayName("Shortcut 목록 조회 - 성공")
    void listShortcuts_Success() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("개발");
        category.setDeleted("N");
        categoryRepository.save(category);

        // When & Then
        mockMvc.perform(get("/shortcuts"))
                .andExpect(status().isOk())
                .andExpect(view().name("shortcut/list"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("newCategory"))
                .andExpect(model().attributeExists("newShortcut"));
    }

    @Test
    @DisplayName("카테고리 생성 - 성공")
    void createCategory_Success() throws Exception {
        // When
        mockMvc.perform(post("/shortcuts/categories")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "업무"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts"))
                .andExpect(flash().attributeExists("success"));

        // Then
        assertThat(categoryRepository.count()).isEqualTo(1);
        ShortcutCategory saved = categoryRepository.findAll().get(0);
        assertThat(saved.getName()).isEqualTo("업무");
    }

    @Test
    @DisplayName("카테고리 생성 - 이름 없음 실패")
    void createCategory_NoName_Fails() throws Exception {
        // When
        mockMvc.perform(post("/shortcuts/categories")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    @DisplayName("카테고리 수정 - 성공")
    void updateCategory_Success() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("기존 카테고리");
        category.setDeleted("N");
        ShortcutCategory saved = categoryRepository.save(category);

        // When
        mockMvc.perform(post("/shortcuts/categories/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "수정된 카테고리"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts?tab=" + saved.getId()))
                .andExpect(flash().attributeExists("success"));

        // Then
        Optional<ShortcutCategory> updated = categoryRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("수정된 카테고리");
    }

    @Test
    @DisplayName("카테고리 삭제 - 성공 (소프트 삭제)")
    void deleteCategory_Success() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("삭제할 카테고리");
        category.setDeleted("N");
        ShortcutCategory saved = categoryRepository.save(category);

        // When
        mockMvc.perform(post("/shortcuts/categories/{id}/delete", saved.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts"))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제 확인
        Optional<ShortcutCategory> deleted = categoryRepository.findById(saved.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    // ========== Shortcut Tests ==========

    @Test
    @DisplayName("단축키 생성 - 성공")
    void createShortcut_Success() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("개발");
        category.setDeleted("N");
        ShortcutCategory savedCategory = categoryRepository.save(category);

        // When
        mockMvc.perform(post("/shortcuts/items")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("categoryId", savedCategory.getId().toString())
                        .param("name", "GitHub")
                        .param("url", "https://github.com")
                        .param("description", "소스코드 저장소"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts?tab=" + savedCategory.getId()))
                .andExpect(flash().attributeExists("success"));

        // Then
        assertThat(shortcutRepository.count()).isEqualTo(1);
        Shortcut saved = shortcutRepository.findAll().get(0);
        assertThat(saved.getName()).isEqualTo("GitHub");
        assertThat(saved.getUrl()).isEqualTo("https://github.com");
        assertThat(saved.getCategory().getId()).isEqualTo(savedCategory.getId());
    }

    @Test
    @DisplayName("단축키 생성 - 필수 필드 없음 실패")
    void createShortcut_NoRequiredFields_Fails() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("개발");
        category.setDeleted("N");
        ShortcutCategory savedCategory = categoryRepository.save(category);

        // When - name 없음
        mockMvc.perform(post("/shortcuts/items")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("categoryId", savedCategory.getId().toString())
                        .param("name", "")
                        .param("url", "https://github.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(shortcutRepository.count()).isZero();
    }

    @Test
    @DisplayName("단축키 생성 - 카테고리 없음 실패")
    void createShortcut_NoCategoryFound_Fails() throws Exception {
        // When
        mockMvc.perform(post("/shortcuts/items")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("categoryId", "99999")
                        .param("name", "GitHub")
                        .param("url", "https://github.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts"))
                .andExpect(flash().attributeExists("error"));

        // Then
        assertThat(shortcutRepository.count()).isZero();
    }

    @Test
    @DisplayName("단축키 수정 - 성공")
    void updateShortcut_Success() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("개발");
        category.setDeleted("N");
        ShortcutCategory savedCategory = categoryRepository.save(category);

        Shortcut shortcut = new Shortcut();
        shortcut.setName("GitHub");
        shortcut.setUrl("https://github.com");
        shortcut.setCategory(savedCategory);
        shortcut.setDeleted("N");
        Shortcut savedShortcut = shortcutRepository.save(shortcut);

        // When
        mockMvc.perform(post("/shortcuts/items/{id}", savedShortcut.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("categoryId", savedCategory.getId().toString())
                        .param("name", "GitLab")
                        .param("url", "https://gitlab.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts?tab=" + savedCategory.getId()))
                .andExpect(flash().attributeExists("success"));

        // Then
        Optional<Shortcut> updated = shortcutRepository.findById(savedShortcut.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getName()).isEqualTo("GitLab");
        assertThat(updated.get().getUrl()).isEqualTo("https://gitlab.com");
    }

    @Test
    @DisplayName("단축키 삭제 - 성공 (소프트 삭제)")
    void deleteShortcut_Success() throws Exception {
        // Given
        ShortcutCategory category = new ShortcutCategory();
        category.setName("개발");
        category.setDeleted("N");
        ShortcutCategory savedCategory = categoryRepository.save(category);

        Shortcut shortcut = new Shortcut();
        shortcut.setName("삭제할 단축키");
        shortcut.setUrl("https://delete.me");
        shortcut.setCategory(savedCategory);
        shortcut.setDeleted("N");
        Shortcut savedShortcut = shortcutRepository.save(shortcut);

        // When
        mockMvc.perform(post("/shortcuts/items/{id}/delete", savedShortcut.getId())
                        .param("categoryId", savedCategory.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shortcuts?tab=" + savedCategory.getId()))
                .andExpect(flash().attributeExists("success"));

        // Then - 소프트 삭제 확인
        Optional<Shortcut> deleted = shortcutRepository.findById(savedShortcut.getId());
        assertThat(deleted).isPresent();
        assertThat(deleted.get().getDeleted()).isEqualTo("Y");
    }

    @Test
    @DisplayName("카테고리와 단축키 함께 조회 - 성공")
    void listShortcutsWithCategories_Success() throws Exception {
        // Given
        ShortcutCategory category1 = new ShortcutCategory();
        category1.setName("개발");
        category1.setDisplayOrder(1);
        category1.setDeleted("N");
        ShortcutCategory savedCat1 = categoryRepository.save(category1);

        ShortcutCategory category2 = new ShortcutCategory();
        category2.setName("문서");
        category2.setDisplayOrder(2);
        category2.setDeleted("N");
        categoryRepository.save(category2);

        Shortcut shortcut1 = new Shortcut();
        shortcut1.setName("GitHub");
        shortcut1.setUrl("https://github.com");
        shortcut1.setCategory(savedCat1);
        shortcut1.setDeleted("N");
        shortcutRepository.save(shortcut1);

        Shortcut shortcut2 = new Shortcut();
        shortcut2.setName("GitLab");
        shortcut2.setUrl("https://gitlab.com");
        shortcut2.setCategory(savedCat1);
        shortcut2.setDeleted("N");
        shortcutRepository.save(shortcut2);

        // When & Then
        mockMvc.perform(get("/shortcuts"))
                .andExpect(status().isOk())
                .andExpect(view().name("shortcut/list"))
                .andExpect(model().attributeExists("categories"));
    }
}
