package com.modestoneto.secretaria;

import java.util.concurrent.ThreadLocalRandom;

/** Gera CPFs com dígitos verificadores válidos, para os testes não colidirem no índice único. */
public final class TestCpf {

    private TestCpf() {
    }

    /** 11 dígitos, sem máscara. */
    public static String novo() {
        while (true) {
            StringBuilder base = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                base.append(ThreadLocalRandom.current().nextInt(10));
            }
            base.append(verificador(base, 9));
            base.append(verificador(base, 10));
            if (base.chars().distinct().count() > 1) {
                return base.toString();
            }
        }
    }

    /** 12345678909 vira 123.456.789-09. */
    public static String mascarado(String digitos) {
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "." + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    private static int verificador(CharSequence digitos, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (digitos.charAt(i) - '0') * (tamanho + 1 - i);
        }
        return (soma * 10 % 11) % 10;
    }
}
