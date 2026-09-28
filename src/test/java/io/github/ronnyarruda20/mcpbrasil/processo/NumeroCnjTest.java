package io.github.ronnyarruda20.mcpbrasil.processo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;

class NumeroCnjTest {

    /** Processo real do TRF1, devolvido pela API pública do DataJud. */
    private static final String REAL = "1000963-07.2021.4.01.4004";

    @Test
    void aceitaNumeroFormatadoOuSoDigitos() {
        var formatado = NumeroCnj.parse(REAL);
        var digitos = NumeroCnj.parse("10009630720214014004");

        assertThat(formatado).isEqualTo(digitos);
        assertThat(formatado.formatado()).isEqualTo(REAL);
        assertThat(formatado.somenteDigitos()).isEqualTo("10009630720214014004");
        assertThat(formatado.siglaDataJud()).isEqualTo("trf1");
    }

    @Test
    void recusaDigitoVerificadorErrado() {
        assertThatThrownBy(() -> NumeroCnj.parse("1000963-08.2021.4.01.4004"))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("dígito verificador");
    }

    @Test
    void recusaTamanhoErrado() {
        assertThatThrownBy(() -> NumeroCnj.parse("123-45.2021"))
                .isInstanceOf(ConsultaException.class)
                .hasMessageContaining("20 dígitos");
    }

    @Test
    void recusaVazio() {
        assertThatThrownBy(() -> NumeroCnj.parse("  ")).isInstanceOf(ConsultaException.class);
    }

    @ParameterizedTest(name = "J={0} TR={1} -> {2}")
    @CsvSource({
            "8, 11, tjmt",
            "8, 26, tjsp",
            "8, 07, tjdft",
            "8, 01, tjac",
            "8, 27, tjto",
            "4, 01, trf1",
            "4, 06, trf6",
            "5, 00, tst",
            "5, 23, trt23",
            "6, 00, tse",
            "6, 07, tre-df",
            "6, 26, tre-sp",
            "9, 13, tjmmg",
            "9, 21, tjmrs",
            "9, 26, tjmsp",
            "3, 00, stj",
            "7, 00, stm",
    })
    void descobreOTribunalPeloNumero(String justica, String tribunal, String sigla) {
        assertThat(numero(justica, tribunal).siglaDataJud()).isEqualTo(sigla);
    }

    @ParameterizedTest(name = "J={0} TR={1}")
    @CsvSource({
            "1, 00",  // STF: fora do DataJud
            "4, 07",  // não existe TRF7
            "5, 25",  // não existe TRT25
            "8, 28",  // não existe 28ª UF
            "9, 11",  // MT não tem TJM
            "2, 00",  // CNJ
    })
    void recusaTribunalInexistenteOuForaDoDataJud(String justica, String tribunal) {
        assertThatThrownBy(() -> numero(justica, tribunal).siglaDataJud()).isInstanceOf(ConsultaException.class);
    }

    private static NumeroCnj numero(String justica, String tribunal) {
        return new NumeroCnj("0000001", "00", "2024", justica, tribunal, "0001");
    }
}
