package com.modestoneto.secretaria.application.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResultTest {

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "20,1", "21,2", "40,2", "41,3"})
    void totalPagesComTamanho20(long total, int paginas) {
        assertThat(new PageResult<>(List.of(), 0, 20, total).totalPages()).isEqualTo(paginas);
    }

    @Test
    void mapConverteOConteudoEMantemOsMetadados() {
        PageResult<Integer> origem = new PageResult<>(List.of(1, 2, 3), 2, 3, 9);

        PageResult<String> convertido = origem.map(n -> "n" + n);

        assertThat(convertido.content()).containsExactly("n1", "n2", "n3");
        assertThat(convertido.page()).isEqualTo(2);
        assertThat(convertido.size()).isEqualTo(3);
        assertThat(convertido.totalElements()).isEqualTo(9);
    }
}
