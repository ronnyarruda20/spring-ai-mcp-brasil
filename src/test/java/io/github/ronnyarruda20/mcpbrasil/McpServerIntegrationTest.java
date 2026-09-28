package io.github.ronnyarruda20.mcpbrasil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.github.ronnyarruda20.mcpbrasil.brasilapi.BrasilApiClient;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Endereco;
import io.github.ronnyarruda20.mcpbrasil.processo.DataJudClient;
import io.github.ronnyarruda20.mcpbrasil.processo.Processo;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Sobe o servidor de verdade e conversa com ele por um cliente MCP pelo
 * transporte Streamable HTTP. As APIs externas são substituídas por mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

    @LocalServerPort
    int porta;

    @MockitoBean
    BrasilApiClient brasilApi;

    @MockitoBean
    DataJudClient dataJud;

    McpSyncClient cliente;

    @BeforeEach
    void conectar() {
        var transporte = HttpClientStreamableHttpTransport.builder("http://localhost:" + porta).build();
        cliente = McpClient.sync(transporte).requestTimeout(Duration.ofSeconds(10)).build();
        cliente.initialize();
    }

    @AfterEach
    void desconectar() {
        cliente.closeGracefully();
    }

    @Test
    void expoeAsQuatroFerramentasComoSomenteLeitura() {
        List<McpSchema.Tool> ferramentas = cliente.listTools().tools();

        assertThat(ferramentas).extracting(McpSchema.Tool::name)
                .containsExactlyInAnyOrder("consultar_processo", "consultar_cnpj", "consultar_cep", "listar_feriados");
        assertThat(ferramentas).allSatisfy(t -> assertThat(t.annotations().readOnlyHint()).isTrue());
        assertThat(ferramentas).filteredOn(t -> t.name().equals("consultar_processo")).singleElement()
                .satisfies(t -> assertThat(t.inputSchema()).extractingByKey("required")
                        .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                        .containsExactly("numero"));
    }

    @Test
    void chamaUmaFerramentaEDevolveOResultadoEmJson() {
        given(brasilApi.cep("78005000")).willReturn(new Endereco("78005000", "Rua Engenheiro Ricardo Franco",
                "Centro-Norte", "Cuiabá", "MT", "5103403", "-15.59611", "-56.09667", "open-cep"));

        var resultado = cliente.callTool(new McpSchema.CallToolRequest("consultar_cep", Map.of("cep", "78005-000")));

        assertThat(resultado.isError()).isFalse();
        assertThat(texto(resultado)).contains("\"cidade\":\"Cuiabá\"").contains("\"uf\":\"MT\"");
    }

    @Test
    void entradaInvalidaVoltaComoErroLegivelParaOModelo() {
        var resultado = cliente.callTool(new McpSchema.CallToolRequest("consultar_processo",
                Map.of("numero", "1000963-08.2021.4.01.4004")));

        assertThat(resultado.isError()).isTrue();
        assertThat(texto(resultado)).contains("dígito verificador");
    }

    @Test
    void consultaDeProcessoPassaOLimiteDeMovimentosAoCliente() {
        given(dataJud.buscar(any(), anyInt())).willReturn(List.of(new Processo("1000963-07.2021.4.01.4004", "TRF1",
                "2º grau", "Apelação Criminal", List.of(), null, null, "PJe", false, null, 0, List.of())));

        var resultado = cliente.callTool(new McpSchema.CallToolRequest("consultar_processo",
                Map.of("numero", "1000963-07.2021.4.01.4004", "limiteMovimentos", 500)));

        assertThat(resultado.isError()).isFalse();
        assertThat(texto(resultado)).contains("Apelação Criminal");
        org.mockito.Mockito.verify(dataJud).buscar(any(), org.mockito.ArgumentMatchers.eq(50));
    }

    private static String texto(McpSchema.CallToolResult resultado) {
        return resultado.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(c -> ((McpSchema.TextContent) c).text())
                .reduce("", String::concat);
    }
}
