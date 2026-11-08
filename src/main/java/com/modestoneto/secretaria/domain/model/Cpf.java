package com.modestoneto.secretaria.domain.model;

import com.modestoneto.secretaria.domain.exception.InvalidRequestException;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * CPF guardado só com os 11 dígitos; a máscara (000.000.000-00) existe apenas na saída. A entrada aceita
 * os dois formatos.
 */
public final class Cpf {

    private static final Pattern SO_DIGITOS_E_MASCARA = Pattern.compile("[\\d.\\-\\s]+");
    private static final int MIN_DIGITOS_BUSCA = 3;

    private Cpf() {
    }

    /** Devolve os 11 dígitos; lança InvalidRequestException se o formato ou os dígitos verificadores falharem. */
    public static String normalize(String raw) {
        if (raw == null || !SO_DIGITOS_E_MASCARA.matcher(raw).matches()) {
            throw invalido();
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1
                || verifier(digits, 9) != digits.charAt(9) - '0'
                || verifier(digits, 10) != digits.charAt(10) - '0') {
            throw invalido();
        }
        return digits;
    }

    /** 12345678909 vira 123.456.789-09; nulo (aluno antigo sem CPF) continua nulo. */
    public static String mask(String digits) {
        if (digits == null) {
            return null;
        }
        return digits.substring(0, 3) + "." + digits.substring(3, 6) + "." + digits.substring(6, 9) + "-" + digits.substring(9);
    }

    /**
     * Se a busca é um CPF (só dígitos e máscara, ao menos 3 dígitos), devolve os dígitos para procurar pelo
     * começo do CPF; senão é busca por nome.
     */
    public static Optional<String> searchDigits(String q) {
        if (q == null || !SO_DIGITOS_E_MASCARA.matcher(q).matches()) {
            return Optional.empty();
        }
        String digits = q.replaceAll("\\D", "");
        return digits.length() >= MIN_DIGITOS_BUSCA && digits.length() <= 11 ? Optional.of(digits) : Optional.empty();
    }

    private static int verifier(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        return (sum * 10 % 11) % 10;
    }

    private static InvalidRequestException invalido() {
        // a mensagem nunca repete o valor recebido (dado pessoal não vai para resposta nem para log)
        return new InvalidRequestException("CPF inválido");
    }
}
