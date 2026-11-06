package com.modestoneto.secretaria.application.common;

import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageRequestTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 20, 100})
    void aceitaTamanhosValidos(int size) {
        assertThat(new PageRequest(0, size, "fullName", SortDirection.ASC).size()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 101, 1000})
    void recusaTamanhosInvalidos(int size) {
        assertThatThrownBy(() -> new PageRequest(0, size, "fullName", SortDirection.ASC))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void recusaPaginaNegativa() {
        assertThatThrownBy(() -> new PageRequest(-1, 20, "fullName", SortDirection.ASC))
                .isInstanceOf(InvalidRequestException.class);
    }
}
