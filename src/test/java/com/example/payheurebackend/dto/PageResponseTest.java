package com.example.payheurebackend.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void of_repriteFidelementLesMetadonneesDuPageSpringData() {
        PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }

    @Test
    void of_dernierePageEstMarqueeCommeTelle() {
        PageImpl<String> page = new PageImpl<>(List.of("c"), PageRequest.of(2, 2), 5);

        PageResponse<String> response = PageResponse.of(page);

        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
    }
}
