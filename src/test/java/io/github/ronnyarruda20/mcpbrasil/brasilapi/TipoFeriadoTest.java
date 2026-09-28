package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TipoFeriadoTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "Carnaval, PONTO_FACULTATIVO",
            "CARNAVAL, PONTO_FACULTATIVO",
            "Corpus Christi, PONTO_FACULTATIVO",
            "Páscoa, DATA_COMEMORATIVA",
            "Pascoa, DATA_COMEMORATIVA",
            "Sexta-feira Santa, FERIADO",
            "Tiradentes, FERIADO",
            "Dia da consciência negra, FERIADO",
            "Natal, FERIADO",
    })
    void classificaPeloNomeIgnorandoAcentoEMaiusculas(String nome, TipoFeriado esperado) {
        assertThat(TipoFeriado.classificar(nome)).isEqualTo(esperado);
    }
}
