package io.github.ronnyarruda20.mcpbrasil.processo;

import java.math.BigInteger;
import java.util.List;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;

/**
 * Número único de processo no padrão CNJ (Resolução CNJ 65/2008):
 * {@code NNNNNNN-DD.AAAA.J.TR.OOOO}.
 *
 * <p>O segmento {@code J} e o código {@code TR} identificam o tribunal, então
 * o próprio número diz em qual índice do DataJud procurar.
 */
public record NumeroCnj(String sequencial, String digito, String ano, String justica, String tribunal, String origem) {

    private static final BigInteger NOVENTA_E_SETE = BigInteger.valueOf(97);

    /** UFs na ordem dos códigos TR da Justiça Estadual e Eleitoral (01 = AC ... 27 = TO). */
    private static final List<String> UFS = List.of(
            "ac", "al", "ap", "am", "ba", "ce", "df", "es", "go", "ma", "mt", "ms", "mg", "pa",
            "pb", "pr", "pe", "pi", "rj", "rn", "rs", "ro", "rr", "sc", "se", "sp", "to");

    /**
     * Aceita o número com ou sem pontuação. Rejeita tamanho errado e dígito verificador inválido.
     */
    public static NumeroCnj parse(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new ConsultaException("Informe o número do processo no padrão CNJ, por exemplo 1000963-07.2021.4.01.4004.");
        }
        String d = entrada.replaceAll("\\D", "");
        if (d.length() != 20) {
            throw new ConsultaException("Número de processo inválido: '" + entrada
                    + "'. O padrão CNJ tem 20 dígitos (NNNNNNN-DD.AAAA.J.TR.OOOO).");
        }
        var numero = new NumeroCnj(d.substring(0, 7), d.substring(7, 9), d.substring(9, 13),
                d.substring(13, 14), d.substring(14, 16), d.substring(16, 20));
        if (!numero.digitoValido()) {
            throw new ConsultaException("Número de processo com dígito verificador inválido: " + numero.formatado()
                    + ". Confira se não há dígito trocado.");
        }
        return numero;
    }

    boolean digitoValido() {
        var valor = new BigInteger(sequencial + ano + justica + tribunal + origem + digito);
        return valor.mod(NOVENTA_E_SETE).intValue() == 1;
    }

    public String somenteDigitos() {
        return sequencial + digito + ano + justica + tribunal + origem;
    }

    public String formatado() {
        return sequencial + "-" + digito + "." + ano + "." + justica + "." + tribunal + "." + origem;
    }

    /**
     * Sigla do tribunal como aparece no nome do índice do DataJud ({@code api_publica_<sigla>}).
     */
    public String siglaDataJud() {
        int tr = Integer.parseInt(tribunal);
        return switch (justica) {
            case "3" -> "stj";
            case "4" -> {
                exigirFaixa(tr, 1, 6, "Justiça Federal");
                yield "trf" + tr;
            }
            case "5" -> {
                if (tr == 0) {
                    yield "tst";
                }
                exigirFaixa(tr, 1, 24, "Justiça do Trabalho");
                yield "trt" + tr;
            }
            case "6" -> tr == 0 ? "tse" : "tre-" + uf(tr, "Justiça Eleitoral");
            case "7" -> "stm";
            case "8" -> {
                String uf = uf(tr, "Justiça Estadual");
                yield uf.equals("df") ? "tjdft" : "tj" + uf;
            }
            case "9" -> switch (tr) {
                case 13 -> "tjmmg";
                case 21 -> "tjmrs";
                case 26 -> "tjmsp";
                default -> throw new ConsultaException("Não existe Tribunal de Justiça Militar com código " + tribunal + ".");
            };
            case "1" -> throw new ConsultaException("Processos do STF não estão na API Pública do DataJud.");
            default -> throw new ConsultaException("Segmento de justiça '" + justica + "' não é atendido pelo DataJud.");
        };
    }

    private void exigirFaixa(int tr, int min, int max, String ramo) {
        if (tr < min || tr > max) {
            throw new ConsultaException("Código de tribunal " + tribunal + " não existe na " + ramo + ".");
        }
    }

    private String uf(int tr, String ramo) {
        exigirFaixa(tr, 1, UFS.size(), ramo);
        return UFS.get(tr - 1);
    }
}
