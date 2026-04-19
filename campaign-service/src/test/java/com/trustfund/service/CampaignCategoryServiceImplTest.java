package com.trustfund.service;

import com.trustfund.client.MediaServiceClient;
import com.trustfund.exception.ResourceNotFoundException;
import com.trustfund.model.CampaignCategory;
import com.trustfund.model.request.CampaignCategoryRequest;
import com.trustfund.model.response.CampaignCategoryResponse;
import com.trustfund.repository.CampaignCategoryRepository;
import com.trustfund.service.impl.CampaignCategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignCategoryServiceImplTest {

    @Mock
    private CampaignCategoryRepository categoryRepository;

    @Mock
    private MediaServiceClient mediaServiceClient;

    private CampaignCategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CampaignCategoryServiceImpl(categoryRepository, mediaServiceClient);
    }

    // ─── getAll ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("returns all categories")
        void returnsAllCategories() {
            CampaignCategory cat1 = buildCategory(1L, "Health", "Medical support", 100L);
            CampaignCategory cat2 = buildCategory(2L, "Education", "Learning support", 200L);

            when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));
            when(mediaServiceClient.getMediaUrl(100L)).thenReturn("http://media.url/icon1.png");
            when(mediaServiceClient.getMediaUrl(200L)).thenReturn("http://media.url/icon2.png");

            List<CampaignCategoryResponse> result = categoryService.getAll();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Health");
            assertThat(result.get(0).getIconUrl()).isEqualTo("http://media.url/icon1.png");
            assertThat(result.get(1).getName()).isEqualTo("Education");
            verify(categoryRepository).findAll();
        }

        @Test
        @DisplayName("returns empty list when no categories exist")
        void returnsEmptyList() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<CampaignCategoryResponse> result = categoryService.getAll();

            assertThat(result).isEmpty();
        }
    }

    // ─── getById ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("returns category when found")
        void returnsCategoryWhenFound() {
            CampaignCategory category = buildCategory(5L, "Disaster Relief", "Emergency help", 300L);
            when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
            when(mediaServiceClient.getMediaUrl(300L)).thenReturn("http://media.url/icon3.png");

            CampaignCategoryResponse result = categoryService.getById(5L);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getName()).isEqualTo("Disaster Relief");
            assertThat(result.getIconUrl()).isEqualTo("http://media.url/icon3.png");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsNotFoundWhenNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Không tìm thấy danh mục với ID: 99");
        }
    }

    // ─── create ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("saves category successfully")
        void savesCategorySuccessfully() {
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("Environment")
                    .description("Environmental protection projects")
                    .icon(500L)
                    .build();

            when(categoryRepository.existsByName("Environment")).thenReturn(false);
            when(categoryRepository.save(any(CampaignCategory.class))).thenAnswer(inv -> {
                CampaignCategory cat = inv.getArgument(0);
                cat.setId(3L);
                cat.setCreatedAt(LocalDateTime.now());
                cat.setUpdatedAt(LocalDateTime.now());
                return cat;
            });
            when(mediaServiceClient.getMediaUrl(500L)).thenReturn("http://media.url/environment.png");

            CampaignCategoryResponse result = categoryService.create(request);

            assertThat(result.getId()).isEqualTo(3L);
            assertThat(result.getName()).isEqualTo("Environment");
            assertThat(result.getIconUrl()).isEqualTo("http://media.url/environment.png");

            ArgumentCaptor<CampaignCategory> catCaptor = ArgumentCaptor.forClass(CampaignCategory.class);
            verify(categoryRepository).save(catCaptor.capture());
            assertThat(catCaptor.getValue().getName()).isEqualTo("Environment");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when category name already exists")
        void throwsWhenNameAlreadyExists() {
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("Health")
                    .description("Duplicate name")
                    .build();

            when(categoryRepository.existsByName("Health")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tên danh mục đã tồn tại");

            verify(categoryRepository, never()).save(any(CampaignCategory.class));
        }

        @Test
        @DisplayName("handles null icon during create")
        void handlesNullIconDuringCreate() {
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("Charity")
                    .description("General charity")
                    .icon(null)
                    .build();

            when(categoryRepository.existsByName("Charity")).thenReturn(false);
            when(categoryRepository.save(any(CampaignCategory.class))).thenAnswer(inv -> {
                CampaignCategory cat = inv.getArgument(0);
                cat.setId(4L);
                cat.setCreatedAt(LocalDateTime.now());
                cat.setUpdatedAt(LocalDateTime.now());
                return cat;
            });

            CampaignCategoryResponse result = categoryService.create(request);

            assertThat(result.getIconUrl()).isNull();
            verify(mediaServiceClient, never()).getMediaUrl(any());
        }
    }

    // ─── update ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("modifies category fields")
        void modifiesCategoryFields() {
            CampaignCategory existingCategory = buildCategory(1L, "Old Name", "Old description", 100L);
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("New Name")
                    .description("New description")
                    .icon(200L)
                    .build();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.existsByName("New Name")).thenReturn(false);
            when(categoryRepository.save(any(CampaignCategory.class))).thenAnswer(inv -> inv.getArgument(0));
            when(mediaServiceClient.getMediaUrl(200L)).thenReturn("http://media.url/new_icon.png");

            CampaignCategoryResponse result = categoryService.update(1L, request);

            assertThat(result.getName()).isEqualTo("New Name");
            assertThat(result.getDescription()).isEqualTo("New description");
            assertThat(result.getIconUrl()).isEqualTo("http://media.url/new_icon.png");
            verify(categoryRepository).save(existingCategory);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when updating non-existent category")
        void throwsNotFoundWhenUpdatingNonExistent() {
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("Name")
                    .description("Desc")
                    .build();

            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.update(99L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Không tìm thấy danh mục với ID: 99");
        }

        @Test
        @DisplayName("throws IllegalArgumentException when new name conflicts with another category")
        void throwsWhenNameConflictsWithAnother() {
            CampaignCategory existingCategory = buildCategory(1L, "Health", "Original", null);
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("Education")
                    .description("Description")
                    .build();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.existsByName("Education")).thenReturn(true);

            assertThatThrownBy(() -> categoryService.update(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tên danh mục đã tồn tại");
        }

        @Test
        @DisplayName("allows updating with same name")
        void allowsUpdatingWithSameName() {
            CampaignCategory existingCategory = buildCategory(1L, "Health", "Old description", null);
            CampaignCategoryRequest request = CampaignCategoryRequest.builder()
                    .name("Health")
                    .description("Updated description")
                    .build();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
            when(categoryRepository.save(any(CampaignCategory.class))).thenAnswer(inv -> inv.getArgument(0));

            CampaignCategoryResponse result = categoryService.update(1L, request);

            assertThat(result.getName()).isEqualTo("Health");
            assertThat(result.getDescription()).isEqualTo("Updated description");
        }
    }

    // ─── delete ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("removes category when it exists")
        void removesCategoryWhenExists() {
            when(categoryRepository.existsById(1L)).thenReturn(true);
            doNothing().when(categoryRepository).deleteById(1L);

            categoryService.delete(1L);

            verify(categoryRepository).deleteById(1L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when deleting non-existent category")
        void throwsNotFoundWhenDeletingNonExistent() {
            when(categoryRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> categoryService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Không tìm thấy danh mục với ID: 99");

            verify(categoryRepository, never()).deleteById(any());
        }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private CampaignCategory buildCategory(Long id, String name, String description, Long icon) {
        return CampaignCategory.builder()
                .id(id)
                .name(name)
                .description(description)
                .icon(icon)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
