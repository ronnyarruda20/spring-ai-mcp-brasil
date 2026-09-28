package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;
import io.github.ronnyarruda20.mcpbrasil.http.Tentativas;

class BrasilApiClientTest {

    private MockRestServiceServer servidor;
    private BrasilApiClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://brasilapi.test/api");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new BrasilApiClient(builder.build(), new Tentativas(2, Duration.ZERO));
    }

    @Test
    void resumeOsDadosDoCnpj() {
        servidor.expect(requestTo("https://brasilapi.test/api/cnpj/v1/00000000000191"))
                .andRespond(json("brasilapi/cnpj-00000000000191.json"));

        var empresa = cliente.cnpj("00000000000191");

        assertThat(empresa.razaoSocial()).isEqualTo("BANCO DO BRASIL SA");
        assertThat(empresa.situacaoCadastral()).isEqualTo("ATIVA");
        assertThat(empresa.atividadePrincipal().cnae()).isEqualTo("6422100");
        assertThat(empresa.atividadesSecundarias()).extracting(Modelos.Atividade::cnae).contains("6499999");
        assertThat(empresa.municipio()).isEqualTo("BRASILIA");
        assertThat(empresa.uf()).isEqualTo("DF");
        assertThat(empresa.endereco()).startsWith("QUADRA SAUN QUADRA 5").contains("ASA NORTE");
        assertThat(empresa.socios()).extracting(Modelos.Socio::nome).containsExactly("SOCIO EXEMPLO 1", "SOCIO EXEMPLO 2");
    }

    @Test
    void cnpjInexistenteViraMensagemClara() {
        servidor.expect(requestTo("https://brasilapi.test/api/cnpj/v1/11222333000181"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> cliente.cnpj("11222333000181"))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("não encontrado");
    }

    @Test
    void resumeOEnderecoDoCep() {
        servidor.expect(requestTo("https://brasilapi.test/api/cep/v2/78005000"))
                .andRespond(json("brasilapi/cep-78005000.json"));

        var endereco = cliente.cep("78005000");

        assertThat(endereco.cidade()).isEqualTo("Cuiabá");
        assertThat(endereco.uf()).isEqualTo("MT");
        assertThat(endereco.logradouro()).isEqualTo("Rua Engenheiro Ricardo Franco");
        assertThat(endereco.codigoIbgeMunicipio()).isEqualTo("5103403");
        assertThat(endereco.latitude()).isNotBlank();
    }

    @Test
    void listaOsFeriadosDoAno() {
        servidor.expect(requestTo("https://brasilapi.test/api/feriados/v1/2026"))
                .andRespond(json("brasilapi/feriados-2026.json"));

        var feriados = cliente.feriados(2026);

        assertThat(feriados).hasSize(14);
        assertThat(feriados.getFirst()).isEqualTo(
                new Modelos.Feriado("2026-01-01", "Confraternização mundial", "quinta-feira", "feriado", false));
        assertThat(feriados).filteredOn(f -> f.tipo().equals("ponto facultativo"))
                .extracting(Modelos.Feriado::data)
                .containsExactly("2026-02-16", "2026-02-17", "2026-06-04"); // Carnaval (2 dias) e Corpus Christi
        assertThat(feriados).filteredOn(f -> f.tipo().equals("data comemorativa"))
                .extracting(Modelos.Feriado::nome).containsExactly("Páscoa");
        assertThat(feriados).filteredOn(f -> f.tipo().equals("feriado")).hasSize(10)
                .extracting(Modelos.Feriado::nome).contains("Sexta-feira Santa", "Dia da consciência negra", "Natal");
        // Em 2026, a Páscoa (05/04) e a Proclamação da República (15/11) caem no domingo.
        assertThat(feriados).filteredOn(Modelos.Feriado::caiNoFimDeSemana)
                .extracting(Modelos.Feriado::nome).containsExactly("Páscoa", "Proclamação da República");
    }

    private static org.springframework.test.web.client.ResponseCreator json(String caminho) {
        return withSuccess(new ClassPathResource(caminho), MediaType.APPLICATION_JSON);
    }
}
