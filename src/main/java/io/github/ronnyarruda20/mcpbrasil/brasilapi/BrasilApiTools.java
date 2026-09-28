package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import java.time.Clock;
import java.time.Year;
import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Empresa;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Endereco;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Feriado;
import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;

@Component
public class BrasilApiTools {

    private final BrasilApiClient brasilApi;
    private final Clock clock;

    @Autowired
    public BrasilApiTools(BrasilApiClient brasilApi) {
        this(brasilApi, Clock.systemDefaultZone());
    }

    BrasilApiTools(BrasilApiClient brasilApi, Clock clock) {
        this.brasilApi = brasilApi;
        this.clock = clock;
    }

    @McpTool(name = "consultar_cnpj",
            title = "Consultar CNPJ",
            description = """
                    Consulta os dados cadastrais públicos de uma empresa na Receita Federal pelo CNPJ: razão social, \
                    nome fantasia, situação cadastral, atividades (CNAE), endereço, porte, capital social, opção pelo \
                    Simples/MEI e quadro de sócios. Aceita CNPJ com ou sem pontuação, inclusive o formato alfanumérico.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = true))
    public Empresa consultarCnpj(
            @McpToolParam(description = "CNPJ com ou sem pontuação. Ex.: 00.000.000/0001-91") String cnpj) {
        return brasilApi.cnpj(Cnpj.normalizar(cnpj));
    }

    @McpTool(name = "consultar_cep",
            title = "Consultar CEP",
            description = "Busca o endereço de um CEP brasileiro: logradouro, bairro, cidade, UF, código IBGE do município e coordenadas quando disponíveis.",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = true))
    public Endereco consultarCep(
            @McpToolParam(description = "CEP com 8 dígitos, com ou sem hífen. Ex.: 78005-000") String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("\\D", "");
        if (digitos.length() != 8) {
            throw new ConsultaException("CEP inválido: '" + cep + "'. O CEP tem 8 dígitos.");
        }
        return brasilApi.cep(digitos);
    }

    @McpTool(name = "listar_feriados",
            title = "Listar feriados nacionais",
            description = """
                    Lista as datas nacionais de um ano, incluindo as móveis, com o tipo de cada uma: "feriado" \
                    (lei federal), "ponto facultativo" (Carnaval e Corpus Christi, que só viram feriado se estado, \
                    município ou tribunal decretar) ou "data comemorativa" (Páscoa). Indica também se a data cai no \
                    fim de semana. Não inclui feriados estaduais, municipais nem suspensões de expediente dos tribunais; \
                    para contar prazo processual, confira também o calendário do tribunal.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = true))
    public List<Feriado> listarFeriados(
            @McpToolParam(required = false, description = "Ano com 4 dígitos, entre 1900 e 2199. Padrão: o ano atual.")
            Integer ano) {
        int alvo = ano == null ? Year.now(clock).getValue() : ano;
        if (alvo < 1900 || alvo > 2199) {
            throw new ConsultaException("Ano fora do intervalo aceito (1900 a 2199): " + alvo + ".");
        }
        return brasilApi.feriados(alvo);
    }
}
