package io.github.ronnyarruda20.mcpbrasil.processo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;
import io.github.ronnyarruda20.mcpbrasil.http.Tentativas;

/**
 * Cliente da API Pública do DataJud (CNJ). Cada tribunal é um índice
 * Elasticsearch separado, então o número CNJ é usado para escolher o índice.
 */
public class DataJudClient {

    private static final Map<String, String> GRAUS = Map.of(
            "G1", "1º grau",
            "G2", "2º grau",
            "JE", "Juizado Especial",
            "TR", "Turma Recursal",
            "SUP", "Tribunal Superior",
            "TRU", "Turma Regional de Uniformização",
            "TNU", "Turma Nacional de Uniformização",
            "CJF", "Conselho da Justiça Federal");

    private final RestClient http;
    private final Tentativas tentativas;

    public DataJudClient(RestClient http, Tentativas tentativas) {
        this.http = http;
        this.tentativas = tentativas;
    }

    /**
     * @param limiteMovimentos quantos andamentos devolver por registro, do mais recente para o mais antigo
     */
    public List<Processo> buscar(NumeroCnj numero, int limiteMovimentos) {
        String sigla = numero.siglaDataJud();
        var corpo = Map.of(
                "size", 10,
                "query", Map.of("match", Map.of("numeroProcesso", numero.somenteDigitos())));

        DataJudResposta resposta;
        try {
            resposta = tentativas.executar(() -> http.post()
                    .uri("/api_publica_{sigla}/_search", sigla)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(DataJudResposta.class));
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            throw new ConsultaException("O DataJud recusou a chave de acesso. O CNJ pode ter trocado a chave pública: "
                    + "veja a atual em https://datajud-wiki.cnj.jus.br/api-publica/acesso e defina DATAJUD_API_KEY.", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ConsultaException("O DataJud está sobrecarregado (HTTP 429) e não respondeu após novas tentativas. "
                    + "Tente de novo em alguns minutos.", e);
        } catch (RestClientException e) {
            throw new ConsultaException("Não foi possível consultar o DataJud (" + sigla.toUpperCase() + "): "
                    + e.getMessage(), e);
        }

        List<DataJudResposta.Hit> hits = resposta == null || resposta.hits() == null || resposta.hits().hits() == null
                ? List.of()
                : resposta.hits().hits();
        if (hits.isEmpty()) {
            throw new ConsultaException("Processo " + numero.formatado() + " não encontrado no DataJud ("
                    + sigla.toUpperCase() + "). Processos sigilosos e alguns muito recentes não aparecem na API pública.");
        }
        return hits.stream()
                .map(DataJudResposta.Hit::_source)
                .filter(Objects::nonNull)
                .map(f -> converter(f, numero, limiteMovimentos))
                .toList();
    }

    static Processo converter(DataJudResposta.Fonte f, NumeroCnj numero, int limite) {
        List<DataJudResposta.Movimento> movimentos = f.movimentos() == null ? List.of() : f.movimentos();
        List<Processo.Movimento> recentes = movimentos.stream()
                .sorted(Comparator.comparing(DataJudResposta.Movimento::dataHora,
                        Comparator.nullsLast(Comparator.<String>reverseOrder())))
                .limit(limite)
                .map(m -> new Processo.Movimento(m.dataHora(), m.nome(), complementos(m)))
                .toList();

        return new Processo(
                numero.formatado(),
                f.tribunal(),
                GRAUS.getOrDefault(f.grau(), f.grau()),
                nome(f.classe()),
                f.assuntos() == null ? List.of() : f.assuntos().stream().map(DataJudClient::nome).toList(),
                f.orgaoJulgador() == null ? null : f.orgaoJulgador().nome(),
                normalizarData(f.dataAjuizamento()),
                nome(f.sistema()),
                f.nivelSigilo() != null && f.nivelSigilo() > 0,
                f.dataHoraUltimaAtualizacao(),
                movimentos.size(),
                recentes);
    }

    private static List<String> complementos(DataJudResposta.Movimento m) {
        if (m.complementosTabelados() == null) {
            return List.of();
        }
        return m.complementosTabelados().stream()
                .map(c -> c.descricao() == null ? c.nome() : c.descricao().replace('_', ' ') + ": " + c.nome())
                .toList();
    }

    private static String nome(DataJudResposta.Codigo c) {
        return c == null ? null : c.nome();
    }

    /** Alguns tribunais mandam {@code yyyyMMddHHmmss}; outros já mandam ISO-8601. */
    static String normalizarData(String data) {
        if (data != null && data.matches("\\d{14}")) {
            return data.substring(0, 4) + "-" + data.substring(4, 6) + "-" + data.substring(6, 8)
                    + "T" + data.substring(8, 10) + ":" + data.substring(10, 12) + ":" + data.substring(12, 14);
        }
        return data;
    }
}
