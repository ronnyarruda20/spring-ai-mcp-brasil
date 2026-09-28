package io.github.ronnyarruda20.mcpbrasil.processo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;
import io.github.ronnyarruda20.mcpbrasil.http.Tentativas;

class DataJudClientTest {

    private static final String URL_TRF1 = "https://datajud.test/api_publica_trf1/_search";
    private static final NumeroCnj NUMERO = NumeroCnj.parse("1000963-07.2021.4.01.4004");

    private MockRestServiceServer servidor;
    private DataJudClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://datajud.test")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "APIKey chave-teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new DataJudClient(builder.build(), new Tentativas(3, Duration.ZERO));
    }

    @Test
    void consultaOIndiceDoTribunalCertoEResumeOProcesso() {
        servidor.expect(requestTo(URL_TRF1))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "APIKey chave-teste"))
                .andExpect(jsonPath("$.query.match.numeroProcesso").value("10009630720214014004"))
                .andRespond(withSuccess(new ClassPathResource("datajud/processo-trf1.json"), MediaType.APPLICATION_JSON));

        var processos = cliente.buscar(NUMERO, 2);

        servidor.verify();
        assertThat(processos).hasSize(1);
        var p = processos.getFirst();
        assertThat(p.numero()).isEqualTo("1000963-07.2021.4.01.4004");
        assertThat(p.tribunal()).isEqualTo("TRF1");
        assertThat(p.grau()).isEqualTo("2º grau");
        assertThat(p.classe()).isEqualTo("Apelação Criminal");
        assertThat(p.assuntos()).containsExactly("Crimes de Responsabilidade");
        assertThat(p.dataAjuizamento()).isEqualTo("2022-07-04T23:47:32");
        assertThat(p.sistema()).isEqualTo("PJe");
        assertThat(p.sigiloso()).isFalse();
        assertThat(p.totalMovimentos()).isEqualTo(3);
        // A fixture vem em ordem cronológica; a ferramenta devolve do mais recente para o mais antigo.
        assertThat(p.movimentos()).extracting(Processo.Movimento::data)
                .containsExactly("2022-07-04T23:47:42.000Z", "2022-07-04T23:47:38.000Z");
    }

    @Test
    void explicaQuandoOProcessoNaoExiste() {
        servidor.expect(requestTo(URL_TRF1))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.buscar(NUMERO, 10))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("não encontrado no DataJud (TRF1)");
    }

    @Test
    void tentaDeNovoQuandoODataJudEstaSobrecarregado() {
        servidor.expect(times(2), requestTo(URL_TRF1)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo(URL_TRF1))
                .andRespond(withSuccess(new ClassPathResource("datajud/processo-trf1.json"), MediaType.APPLICATION_JSON));

        assertThat(cliente.buscar(NUMERO, 10)).hasSize(1);
        servidor.verify();
    }

    @Test
    void mensagemClaraQuandoASobrecargaPersiste() {
        servidor.expect(times(3), requestTo(URL_TRF1)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> cliente.buscar(NUMERO, 10))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("sobrecarregado");
    }

    @Test
    void avisaQuandoAChavePublicaFoiTrocada() {
        servidor.expect(requestTo(URL_TRF1)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> cliente.buscar(NUMERO, 10))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("DATAJUD_API_KEY");
    }
}
