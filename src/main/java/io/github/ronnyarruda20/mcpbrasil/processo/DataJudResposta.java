package io.github.ronnyarruda20.mcpbrasil.processo;

import java.util.List;

/**
 * Formato bruto devolvido pelo Elasticsearch do DataJud. Só os campos usados
 * aqui; o resto é ignorado na desserialização.
 */
record DataJudResposta(Hits hits) {

    record Hits(List<Hit> hits) {
    }

    record Hit(Fonte _source) {
    }

    record Fonte(
            String numeroProcesso,
            String tribunal,
            String grau,
            String dataAjuizamento,
            Integer nivelSigilo,
            String dataHoraUltimaAtualizacao,
            Codigo classe,
            Codigo sistema,
            Codigo formato,
            OrgaoJulgador orgaoJulgador,
            List<Codigo> assuntos,
            List<Movimento> movimentos) {
    }

    record Codigo(Object codigo, String nome) {
    }

    record OrgaoJulgador(Object codigo, String nome, Object codigoMunicipioIBGE) {
    }

    record Movimento(Object codigo, String nome, String dataHora, List<Complemento> complementosTabelados) {
    }

    record Complemento(String descricao, String nome) {
    }
}
