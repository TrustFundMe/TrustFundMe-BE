package com.trustfund.repository;

import com.trustfund.model.ModuleGroup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class ModuleGroupRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private ModuleGroupRepository repo;

    private ModuleGroup persist(String name, Integer order, Boolean active) {
        ModuleGroup g = ModuleGroup.builder().name(name).description("d")
                .displayOrder(order).isActive(active).build();
        return em.persistAndFlush(g);
    }

    @Test @DisplayName("findAllByOrderByDisplayOrderAsc_sortsAsc")
    void ordered() {
        persist("B", 2, true);
        persist("A", 1, true);
        assertThat(repo.findAllByOrderByDisplayOrderAsc())
                .extracting(ModuleGroup::getName).containsExactly("A", "B");
    }

    @Test @DisplayName("existsByName_true")
    void existsByName() {
        persist("MyGroup", 1, true);
        assertThat(repo.existsByName("MyGroup")).isTrue();
    }

    @Test @DisplayName("existsByName_false")
    void existsByName_false() { assertThat(repo.existsByName("NoSuch")).isFalse(); }

    @Test @DisplayName("existsByNameIgnoreCase_caseInsensitive")
    void existsIgnoreCase() {
        persist("MyGroup", 1, true);
        assertThat(repo.existsByNameIgnoreCase("mygroup")).isTrue();
    }

    @Test @DisplayName("findByIsActiveTrueOrderByDisplayOrderAsc_filters")
    void activeOnly() {
        persist("A", 1, true);
        persist("B", 2, false);
        assertThat(repo.findByIsActiveTrueOrderByDisplayOrderAsc()).hasSize(1);
    }

    @Test @DisplayName("search_byKeyword")
    void search() {
        persist("Admin Tools", 1, true);
        persist("User Tools", 2, true);
        assertThat(repo.search("%admin%", null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1L);
    }

    @Test @Transactional @DisplayName("shiftOrdersForInsert_incrementsAll")
    void shiftInsert() {
        persist("A", 1, true);
        persist("B", 2, true);
        repo.shiftOrdersForInsert(1);
        em.flush(); em.clear();
        assertThat(repo.findAllByOrderByDisplayOrderAsc())
                .extracting(ModuleGroup::getDisplayOrder).containsExactly(2, 3);
    }
}
