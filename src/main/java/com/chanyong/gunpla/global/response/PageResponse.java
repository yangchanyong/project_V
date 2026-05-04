package com.chanyong.gunpla.global.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
    List<T> data,
    PageInfo page
) {
    public record PageInfo(int number, int size, long totalElements, int totalPages) {}

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            new PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages())
        );
    }
}
