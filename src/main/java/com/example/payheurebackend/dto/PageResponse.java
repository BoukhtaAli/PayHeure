package com.example.payheurebackend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page de résultats exposée par l'API. On évite de sérialiser directement un {@link Page}
 * Spring Data pour garder un contrat stable, indépendant de l'implémentation de pagination.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
