package io.github.ronnyarruda20.mcpbrasil.http;

import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Repete uma chamada HTTP quando a falha é transitória: 429, 5xx ou timeout.
 * Erros 4xx (exceto 429) voltam na hora, porque repetir não muda o resultado.
 */
public class Tentativas {

    private final int max;
    private final Duration esperaInicial;

    public Tentativas(int max, Duration esperaInicial) {
        if (max < 1) {
            throw new IllegalArgumentException("max deve ser >= 1");
        }
        this.max = max;
        this.esperaInicial = esperaInicial;
    }

    public <T> T executar(Supplier<T> chamada) {
        RuntimeException ultima = null;
        Duration espera = esperaInicial;
        for (int tentativa = 1; tentativa <= max; tentativa++) {
            try {
                return chamada.get();
            } catch (HttpStatusCodeException e) {
                if (!transitoria(e)) {
                    throw e;
                }
                ultima = e;
            } catch (ResourceAccessException e) {
                ultima = e;
            }
            if (tentativa < max) {
                dormir(espera);
                espera = espera.multipliedBy(2);
            }
        }
        throw ultima;
    }

    private static boolean transitoria(HttpStatusCodeException e) {
        int status = e.getStatusCode().value();
        return status == 429 || status >= 500;
    }

    private static void dormir(Duration d) {
        if (d.isZero() || d.isNegative()) {
            return;
        }
        try {
            Thread.sleep(d);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ConsultaException("Consulta interrompida.", ie);
        }
    }
}
