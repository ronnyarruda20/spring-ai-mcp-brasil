package io.github.ronnyarruda20.mcpbrasil.http;

/**
 * Falha que deve chegar ao modelo como mensagem legível: entrada inválida,
 * registro não encontrado ou API fora do ar. O servidor MCP devolve a mensagem
 * como resultado de erro da ferramenta, e o modelo pode explicá-la ao usuário.
 */
public class ConsultaException extends RuntimeException {

    public ConsultaException(String message) {
        super(message);
    }

    public ConsultaException(String message, Throwable cause) {
        super(message, cause);
    }
}
