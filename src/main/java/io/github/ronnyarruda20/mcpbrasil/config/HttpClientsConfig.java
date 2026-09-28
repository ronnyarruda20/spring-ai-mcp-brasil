package io.github.ronnyarruda20.mcpbrasil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import io.github.ronnyarruda20.mcpbrasil.brasilapi.BrasilApiClient;
import io.github.ronnyarruda20.mcpbrasil.http.Tentativas;
import io.github.ronnyarruda20.mcpbrasil.processo.DataJudClient;

@Configuration(proxyBeanMethods = false)
public class HttpClientsConfig {

    private static final String USER_AGENT = "spring-ai-mcp-brasil (+https://github.com/ronnyarruda20/spring-ai-mcp-brasil)";

    @Bean
    Tentativas tentativas(McpBrasilProperties props) {
        return new Tentativas(props.http().maxTentativas(), props.http().espera());
    }

    @Bean
    DataJudClient dataJudClient(RestClient.Builder builder, McpBrasilProperties props, Tentativas tentativas) {
        RestClient client = builder.clone()
                .requestFactory(requestFactory(props))
                .baseUrl(props.datajud().baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "APIKey " + props.datajud().apiKey())
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
        return new DataJudClient(client, tentativas);
    }

    @Bean
    BrasilApiClient brasilApiClient(RestClient.Builder builder, McpBrasilProperties props, Tentativas tentativas) {
        RestClient client = builder.clone()
                .requestFactory(requestFactory(props))
                .baseUrl(props.brasilApi().baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
        return new BrasilApiClient(client, tentativas);
    }

    private static JdkClientHttpRequestFactory requestFactory(McpBrasilProperties props) {
        var httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(props.http().connectTimeout())
                .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(props.http().readTimeout());
        return factory;
    }
}
