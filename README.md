# spring-ai-mcp-brasil

[![CI](https://github.com/ronnyarruda20/spring-ai-mcp-brasil/actions/workflows/ci.yml/badge.svg)](https://github.com/ronnyarruda20/spring-ai-mcp-brasil/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0-6db33f)
![Spring AI 2](https://img.shields.io/badge/Spring%20AI-2.0-6db33f)

Servidor **MCP (Model Context Protocol)** em Java com **Spring AI** que dá a assistentes de IA acesso a dados públicos brasileiros: processos judiciais do **DataJud/CNJ**, **CNPJ**, **CEP** e **feriados nacionais**.

Com ele conectado, você pergunta ao Claude (ou a qualquer cliente MCP) coisas como:

> "Qual o último andamento do processo 1000963-07.2021.4.01.4004?"
>
> "Essa empresa, 00.000.000/0001-91, está ativa? Quem são os sócios?"
>
> "Quantos dias úteis tem entre 10/11 e 30/11 de 2026?"

e o modelo chama as ferramentas certas, com dados oficiais, em vez de adivinhar.

## Ferramentas

| Ferramenta | O que faz | Fonte |
|---|---|---|
| `consultar_processo` | Metadados e andamentos de um processo: tribunal, grau, classe, assuntos, órgão julgador, movimentações | [API Pública do DataJud (CNJ)](https://datajud-wiki.cnj.jus.br/api-publica/) |
| `consultar_cnpj` | Razão social, situação cadastral, CNAE, endereço, porte, Simples/MEI e quadro de sócios | [BrasilAPI](https://brasilapi.com.br) / Receita Federal |
| `consultar_cep` | Logradouro, bairro, cidade, UF, código IBGE e coordenadas | BrasilAPI |
| `listar_feriados` | Datas nacionais do ano, separando feriado, ponto facultativo e data comemorativa, e indicando as que caem no fim de semana | BrasilAPI |

Todas são somente leitura e declaram isso ao cliente (`readOnlyHint`).

## Decisões de implementação

- **O tribunal sai do próprio número CNJ.** O DataJud tem um índice por tribunal (91 no total). O segmento `J` e o código `TR` do número `NNNNNNN-DD.AAAA.J.TR.OOOO` identificam qual consultar: `8.11` é o TJMT, `4.01` o TRF1, `5.00` o TST, `6.07` o TRE-DF. O usuário não precisa saber nada disso. Veja [`NumeroCnj`](src/main/java/io/github/ronnyarruda20/mcpbrasil/processo/NumeroCnj.java).
- **Validação antes de sair para a rede.** Número CNJ (dígito verificador módulo 97, Resolução CNJ 65/2008) e CNPJ são validados localmente. Um dígito trocado vira uma mensagem clara para o modelo, e não uma consulta inútil.
- **CNPJ alfanumérico.** A validação já aceita o formato que a Receita passou a emitir em julho de 2026 (IN RFB 2.229/2024), testada com o exemplo oficial `12.ABC.345/01DE-35`.
- **Nova tentativa em sobrecarga.** O DataJud devolve HTTP 429 com frequência. Chamadas com 429, 5xx ou timeout são repetidas com espera exponencial; erros 4xx voltam na hora. Veja [`Tentativas`](src/main/java/io/github/ronnyarruda20/mcpbrasil/http/Tentativas.java).
- **Erros que o modelo entende.** Falhas viram resultado de erro da ferramenta com texto em português ("CNPJ com dígito verificador inválido", "o DataJud está sobrecarregado, tente em alguns minutos"), para o assistente explicar ao usuário em vez de travar.
- **Feriado não é tudo igual.** A BrasilAPI lista Carnaval e Corpus Christi como feriados nacionais, mas pela lei federal eles são ponto facultativo, e a Páscoa é só um domingo. A ferramenta classifica cada data como no calendário oficial do governo federal, para o modelo não contar errado um prazo em dias úteis. Veja [`TipoFeriado`](src/main/java/io/github/ronnyarruda20/mcpbrasil/brasilapi/TipoFeriado.java).
- **Menos dados pessoais.** O resumo de CNPJ não devolve telefone nem e-mail.

## Como rodar

Requer Java 21.

```bash
mvn package
```

**Como servidor HTTP** (Streamable HTTP em `/mcp`, porta 8080):

```bash
java -jar target/spring-ai-mcp-brasil-0.1.0.jar
```

**Como processo local via stdio**, que é o modo que o Claude Desktop usa:

```bash
java -jar target/spring-ai-mcp-brasil-0.1.0.jar --spring.profiles.active=stdio
```

**Com Docker:**

```bash
docker build -t spring-ai-mcp-brasil .
docker run -p 8080:8080 spring-ai-mcp-brasil
```

## Conectando a um assistente

**Claude Code**, com o servidor HTTP rodando:

```bash
claude mcp add --transport http mcp-brasil http://localhost:8080/mcp
```

**Claude Code** via stdio, sem servidor rodando:

```bash
claude mcp add mcp-brasil -- java -jar /caminho/para/spring-ai-mcp-brasil-0.1.0.jar --spring.profiles.active=stdio
```

**Claude Desktop**, em `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "mcp-brasil": {
      "command": "java",
      "args": ["-jar", "/caminho/para/spring-ai-mcp-brasil-0.1.0.jar", "--spring.profiles.active=stdio"]
    }
  }
}
```

## Configuração

| Variável | Padrão | Para quê |
|---|---|---|
| `DATAJUD_API_KEY` | chave pública atual do CNJ | O CNJ publica uma chave de acesso aberta e pode trocá-la. Se as consultas de processo começarem a falhar com "recusou a chave", pegue a nova em [datajud-wiki.cnj.jus.br/api-publica/acesso](https://datajud-wiki.cnj.jus.br/api-publica/acesso). |
| `PORT` | `8080` | Porta do servidor HTTP. |

Timeouts e número de tentativas ficam em `mcp-brasil.http.*` no [`application.yml`](src/main/resources/application.yml).

## Testes

```bash
mvn verify
```

- **Unitários:** número CNJ e mapeamento para os 91 tribunais, dígitos verificadores de CNJ e CNPJ, política de novas tentativas.
- **Clientes HTTP:** respostas reais do DataJud e da BrasilAPI gravadas como fixtures, com `MockRestServiceServer`, cobrindo sucesso, 404, 401 e 429.
- **Ponta a ponta:** [`McpServerIntegrationTest`](src/test/java/io/github/ronnyarruda20/mcpbrasil/McpServerIntegrationTest.java) sobe o servidor numa porta aleatória e conversa com ele por um cliente MCP real: lista as ferramentas, chama, e confere o resultado e os erros.

O CI também constrói a imagem Docker e verifica que o servidor responde ao `initialize` do protocolo.

## Limitações

- O DataJud traz **metadados e andamentos**, não o conteúdo das peças nem o nome das partes. Processos sigilosos não aparecem.
- O STF não está na API pública do DataJud.
- Feriados são só os nacionais. Estaduais, municipais e suspensões de expediente dos tribunais ficam de fora.
- Os dados vêm de APIs públicas de terceiros, sem garantia de disponibilidade. Não use como única fonte para contagem de prazo processual.

## Stack

Java 21 · Spring Boot 4.0 · Spring AI 2.0 (MCP Server, anotações `@McpTool`) · MCP Java SDK 2.0 · RestClient · JUnit 5 · GitHub Actions · Docker

## Licença

[MIT](LICENSE)
