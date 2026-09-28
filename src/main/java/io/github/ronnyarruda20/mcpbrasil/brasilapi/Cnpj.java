package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import java.util.Locale;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;

/**
 * Validação de CNPJ, incluindo o formato alfanumérico que a Receita Federal
 * passou a emitir em julho de 2026 (IN RFB 2.229/2024): os 12 primeiros
 * caracteres podem ser letras ou dígitos, e os 2 verificadores continuam numéricos.
 */
public final class Cnpj {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private Cnpj() {
    }

    /** Remove pontuação, valida e devolve os 14 caracteres em maiúsculas. */
    public static String normalizar(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new ConsultaException("Informe o CNPJ, por exemplo 00.000.000/0001-91.");
        }
        String c = entrada.replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
        if (!c.matches("[0-9A-Z]{12}\\d{2}")) {
            throw new ConsultaException("CNPJ inválido: '" + entrada
                    + "'. São 14 caracteres: 12 letras ou dígitos e 2 dígitos verificadores.");
        }
        if (c.chars().distinct().count() == 1) {
            throw new ConsultaException("CNPJ inválido: '" + entrada + "'.");
        }
        String base = c.substring(0, 12);
        char dv1 = digito(base, PESOS_1);
        char dv2 = digito(base + dv1, PESOS_2);
        if (c.charAt(12) != dv1 || c.charAt(13) != dv2) {
            throw new ConsultaException("CNPJ com dígito verificador inválido: '" + entrada + "'.");
        }
        return c;
    }

    /** Letras valem o código ASCII menos 48, como define a Receita; dígitos valem eles mesmos. */
    private static char digito(String s, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (s.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return (char) ('0' + (resto < 2 ? 0 : 11 - resto));
    }
}
