package io.github.ronnyarruda20.mcpbrasil.processo;

import java.util.List;

/**
 * Resumo de um processo como devolvido pela ferramenta {@code consultar_processo}.
 * Um mesmo número pode ter mais de um registro, um por grau de jurisdição.
 */
public record Processo(
        String numero,
        String tribunal,
        String grau,
        String classe,
        List<String> assuntos,
        String orgaoJulgador,
        String dataAjuizamento,
        String sistema,
        boolean sigiloso,
        String ultimaAtualizacao,
        int totalMovimentos,
        List<Movimento> movimentos) {

    public record Movimento(String data, String nome, List<String> complementos) {
    }
}
