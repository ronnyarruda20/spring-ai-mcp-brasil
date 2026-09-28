package io.github.ronnyarruda20.mcpbrasil.processo;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class ProcessoTools {

    static final int MOVIMENTOS_PADRAO = 10;
    static final int MOVIMENTOS_MAXIMO = 50;

    private final DataJudClient dataJud;

    public ProcessoTools(DataJudClient dataJud) {
        this.dataJud = dataJud;
    }

    @McpTool(name = "consultar_processo",
            title = "Consultar processo judicial (DataJud/CNJ)",
            description = """
                    Consulta os metadados públicos de um processo judicial brasileiro na API Pública do DataJud (CNJ): \
                    tribunal, grau, classe, assuntos, órgão julgador, data de ajuizamento e os andamentos mais recentes. \
                    O tribunal é descoberto pelo próprio número CNJ. Atende Justiça Estadual, Federal, do Trabalho, \
                    Eleitoral, Militar, STJ, TST, TSE e STM; não atende o STF. Não traz o conteúdo das peças nem \
                    nomes das partes, e processos sigilosos não aparecem. Pode devolver mais de um registro, um por grau.""",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = true))
    public List<Processo> consultarProcesso(
            @McpToolParam(description = "Número do processo no padrão CNJ, com ou sem pontuação. Ex.: 1000963-07.2021.4.01.4004")
            String numero,
            @McpToolParam(required = false,
                    description = "Quantos andamentos devolver por registro, do mais recente para o mais antigo. Padrão 10, máximo 50.")
            Integer limiteMovimentos) {

        int limite = limiteMovimentos == null ? MOVIMENTOS_PADRAO : Math.clamp(limiteMovimentos, 0, MOVIMENTOS_MAXIMO);
        return dataJud.buscar(NumeroCnj.parse(numero), limite);
    }
}
