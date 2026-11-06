package com.modestoneto.secretaria.adapter.in.web;

import com.modestoneto.secretaria.application.common.PageResult;

import java.util.List;

record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(result.content(), result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
