package com.modestoneto.secretaria.application.common;

import com.modestoneto.secretaria.domain.exception.InvalidRequestException;

/** Paginação própria: a application não pode importar Spring Data. */
public record PageRequest(int page, int size, String sort, SortDirection direction) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        if (page < 0) {
            throw new InvalidRequestException("page deve ser maior ou igual a 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("size deve estar entre 1 e " + MAX_SIZE);
        }
    }
}
