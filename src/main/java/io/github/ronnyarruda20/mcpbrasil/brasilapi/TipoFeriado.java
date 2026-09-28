package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Classifica as datas que a BrasilAPI devolve como "national".
 *
 * <p>A BrasilAPI lista na mesma categoria datas com status legal diferente.
 * A classificação aqui segue o calendário que o governo federal publica todo ano
 * (portaria do Ministério da Gestão para a administração pública federal):
 * <ul>
 *   <li><b>feriado</b>: definido em lei federal (Leis 662/1949, 6.802/1980, 14.759/2023)
 *       ou listado como feriado nacional no calendário oficial, caso da Sexta-feira Santa;</li>
 *   <li><b>ponto facultativo</b>: Carnaval e Corpus Christi. Não são feriados por lei federal;
 *       estados, municípios e tribunais podem decretar feriado nesses dias;</li>
 *   <li><b>data comemorativa</b>: Páscoa, que sempre cai no domingo e não é feriado legal.</li>
 * </ul>
 */
public enum TipoFeriado {

    FERIADO("feriado"),
    PONTO_FACULTATIVO("ponto facultativo"),
    DATA_COMEMORATIVA("data comemorativa");

    private final String descricao;

    TipoFeriado(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public static TipoFeriado classificar(String nome) {
        String n = semAcento(nome);
        if (n.startsWith("carnaval") || n.startsWith("corpus christi")) {
            return PONTO_FACULTATIVO;
        }
        if (n.equals("pascoa")) {
            return DATA_COMEMORATIVA;
        }
        return FERIADO;
    }

    private static String semAcento(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
