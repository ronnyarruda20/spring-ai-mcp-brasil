package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Respostas da BrasilAPI e os resumos devolvidos pelas ferramentas. */
public final class Modelos {

    private Modelos() {
    }

    // ---------- CNPJ ----------

    record CnpjBrasilApi(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            @JsonProperty("descricao_situacao_cadastral") String situacaoCadastral,
            @JsonProperty("data_situacao_cadastral") String dataSituacaoCadastral,
            @JsonProperty("data_inicio_atividade") String dataInicioAtividade,
            @JsonProperty("descricao_identificador_matriz_filial") String matrizFilial,
            @JsonProperty("natureza_juridica") String naturezaJuridica,
            String porte,
            @JsonProperty("capital_social") BigDecimal capitalSocial,
            @JsonProperty("cnae_fiscal") Object cnaeFiscal,
            @JsonProperty("cnae_fiscal_descricao") String cnaeFiscalDescricao,
            @JsonProperty("cnaes_secundarios") List<CnaeBrasilApi> cnaesSecundarios,
            @JsonProperty("descricao_tipo_de_logradouro") String tipoLogradouro,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cep,
            String municipio,
            String uf,
            @JsonProperty("opcao_pelo_simples") Boolean opcaoPeloSimples,
            @JsonProperty("opcao_pelo_mei") Boolean opcaoPeloMei,
            List<SocioBrasilApi> qsa) {
    }

    record CnaeBrasilApi(Object codigo, String descricao) {
    }

    record SocioBrasilApi(
            @JsonProperty("nome_socio") String nome,
            @JsonProperty("qualificacao_socio") String qualificacao,
            @JsonProperty("data_entrada_sociedade") String dataEntrada) {
    }

    /** Resumo devolvido por {@code consultar_cnpj}. Telefone e e-mail ficam de fora de propósito. */
    public record Empresa(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String situacaoCadastral,
            String dataSituacaoCadastral,
            String dataInicioAtividade,
            String matrizOuFilial,
            String naturezaJuridica,
            String porte,
            BigDecimal capitalSocial,
            Atividade atividadePrincipal,
            List<Atividade> atividadesSecundarias,
            String endereco,
            String municipio,
            String uf,
            String cep,
            Boolean optanteSimples,
            Boolean optanteMei,
            List<Socio> socios) {
    }

    public record Atividade(String cnae, String descricao) {
    }

    public record Socio(String nome, String qualificacao, String desde) {
    }

    // ---------- CEP ----------

    record CepBrasilApi(String cep, String state, String city, String neighborhood, String street,
            String service, Ibge ibge, Localizacao location) {

        record Ibge(String city, String state) {
        }

        record Localizacao(Coordenadas coordinates) {
        }

        record Coordenadas(String latitude, String longitude) {
        }
    }

    /** Resumo devolvido por {@code consultar_cep}. */
    public record Endereco(String cep, String logradouro, String bairro, String cidade, String uf,
            String codigoIbgeMunicipio, String latitude, String longitude, String fonte) {
    }

    // ---------- Feriados ----------

    record FeriadoBrasilApi(String date, String name, String type, String weekday) {
    }

    /**
     * Item devolvido por {@code listar_feriados}.
     *
     * @param tipo             "feriado", "ponto facultativo" ou "data comemorativa" (ver {@link TipoFeriado})
     * @param caiNoFimDeSemana true quando a data é sábado ou domingo e, portanto, não tira um dia útil
     */
    public record Feriado(String data, String nome, String diaDaSemana, String tipo, boolean caiNoFimDeSemana) {
    }
}
