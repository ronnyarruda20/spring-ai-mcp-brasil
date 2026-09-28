package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;

class CnpjTest {

    @Test
    void aceitaCnpjNumericoComOuSemPontuacao() {
        assertThat(Cnpj.normalizar("00.000.000/0001-91")).isEqualTo("00000000000191");
        assertThat(Cnpj.normalizar("00000000000191")).isEqualTo("00000000000191");
    }

    @Test
    void aceitaCnpjAlfanumericoDoExemploDaReceita() {
        // Exemplo oficial da Receita Federal para o CNPJ alfanumérico: 12.ABC.345/01DE-35
        assertThat(Cnpj.normalizar("12.ABC.345/01DE-35")).isEqualTo("12ABC34501DE35");
        assertThat(Cnpj.normalizar("12abc34501de35")).isEqualTo("12ABC34501DE35");
    }

    @ParameterizedTest
    @ValueSource(strings = {"00.000.000/0001-92", "12.ABC.345/01DE-36"})
    void recusaDigitoVerificadorErrado(String cnpj) {
        assertThatThrownBy(() -> Cnpj.normalizar(cnpj))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("dígito verificador");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "00.000.000/0001-9A", "11111111111111", "0000000000019100"})
    void recusaFormatoInvalido(String cnpj) {
        assertThatThrownBy(() -> Cnpj.normalizar(cnpj)).isInstanceOf(ConsultaException.class);
    }
}
