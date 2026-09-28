package io.github.ronnyarruda20.mcpbrasil.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuração das APIs públicas consultadas pelo servidor.
 *
 * @param datajud   API Pública do DataJud (CNJ)
 * @param brasilApi BrasilAPI (CNPJ, CEP, feriados)
 * @param http      timeouts e política de nova tentativa comuns aos dois clientes
 */
@ConfigurationProperties("mcp-brasil")
public record McpBrasilProperties(
        @DefaultValue Datajud datajud,
        @DefaultValue BrasilApi brasilApi,
        @DefaultValue Http http) {

    /**
     * @param baseUrl endereço da API Pública do DataJud
     * @param apiKey  chave pública divulgada pelo CNJ em datajud-wiki.cnj.jus.br/api-publica/acesso.
     *                O CNJ pode trocá-la; nesse caso, defina a variável DATAJUD_API_KEY.
     */
    public record Datajud(
            @DefaultValue("https://api-publica.datajud.cnj.jus.br") String baseUrl,
            @DefaultValue("cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw==") String apiKey) {
    }

    public record BrasilApi(@DefaultValue("https://brasilapi.com.br/api") String baseUrl) {
    }

    /**
     * @param connectTimeout tempo máximo para abrir a conexão
     * @param readTimeout    tempo máximo esperando a resposta
     * @param maxTentativas  tentativas quando a API responde 429 ou 5xx (o DataJud fica sobrecarregado com frequência)
     * @param espera         espera antes da segunda tentativa; dobra a cada nova tentativa
     */
    public record Http(
            @DefaultValue("5s") Duration connectTimeout,
            @DefaultValue("20s") Duration readTimeout,
            @DefaultValue("3") int maxTentativas,
            @DefaultValue("1s") Duration espera) {
    }
}
