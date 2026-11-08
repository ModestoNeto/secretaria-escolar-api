package com.modestoneto.secretaria.domain.model;

import com.modestoneto.secretaria.TestCpf;
import com.modestoneto.secretaria.domain.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfTest {

    // 111.444.777-35 e 529.982.247-25 são os CPFs de exemplo mais usados em documentação
    @ParameterizedTest
    @ValueSource(strings = {"11144477735", "111.444.777-35", " 111.444.777-35 ", "529.982.247-25", "52998224725"})
    void aceitaComEmSemMascaraEDevolveSoOsDigitos(String entrada) {
        assertThat(Cpf.normalize(entrada)).matches("\\d{11}");
    }

    @Test
    void normalizaParaOsMesmosDigitos() {
        assertThat(Cpf.normalize("111.444.777-35")).isEqualTo("11144477735");
        assertThat(Cpf.normalize("11144477735")).isEqualTo("11144477735");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "11144477736",       // dígito verificador errado
            "11144477745",       // outro dígito errado
            "00000000000", "11111111111", "99999999999", // todos iguais
            "1114447773",        // curto
            "111444777355",      // longo
            "111.444.777-3x",    // letra
            "111/444/777-35",    // máscara estranha
            "abc"})
    void recusaInvalidos(String entrada) {
        assertThatThrownBy(() -> Cpf.normalize(entrada))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("CPF inválido"); // a mensagem nunca repete o valor
    }

    @Test
    void aceitaOsCpfsGeradosPeloHelperDeTeste() {
        for (int i = 0; i < 200; i++) {
            String cpf = TestCpf.novo();
            assertThat(Cpf.normalize(cpf)).isEqualTo(cpf);
        }
    }

    @Test
    void mascara() {
        assertThat(Cpf.mask("11144477735")).isEqualTo("111.444.777-35");
        assertThat(Cpf.mask("00000001090")).isEqualTo("000.000.010-90");
        assertThat(Cpf.mask(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "123.4", "123.456.789-09", "12345678909", " 123 456 "})
    void buscaComSoDigitosEMascaraEhPorCpf(String q) {
        assertThat(Cpf.searchDigits(q)).isPresent().get().asString().matches("\\d{3,11}");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12", "ana", "ana 123", "123456789012", "jose", "%", "_", "1a3"})
    void outrasBuscasSaoPorNome(String q) {
        assertThat(Cpf.searchDigits(q)).isEmpty();
    }

    @Test
    void buscaDevolveOsDigitosSemMascara() {
        assertThat(Cpf.searchDigits("111.444")).contains("111444");
        assertThat(Cpf.searchDigits("111.444.777-35")).contains("11144477735");
    }
}
