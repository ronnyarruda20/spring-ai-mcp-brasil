package io.github.ronnyarruda20.mcpbrasil.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class TentativasTest {

    private final Tentativas tentativas = new Tentativas(3, Duration.ZERO);

    @Test
    void repeteQuandoAApiEstaSobrecarregadaEDevolveOSucesso() {
        var chamadas = new AtomicInteger();

        String resultado = tentativas.executar(() -> {
            if (chamadas.incrementAndGet() < 3) {
                throw HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "429", null, null, null);
            }
            return "ok";
        });

        assertThat(resultado).isEqualTo("ok");
        assertThat(chamadas).hasValue(3);
    }

    @Test
    void desisteDepoisDoMaximoERepassaAUltimaFalha() {
        var chamadas = new AtomicInteger();

        assertThatThrownBy(() -> tentativas.executar(() -> {
            chamadas.incrementAndGet();
            throw HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "502", null, null, null);
        })).isInstanceOf(HttpServerErrorException.class);
        assertThat(chamadas).hasValue(3);
    }

    @Test
    void naoRepeteErroDoCliente() {
        var chamadas = new AtomicInteger();

        assertThatThrownBy(() -> tentativas.executar(() -> {
            chamadas.incrementAndGet();
            throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "404", null, null, null);
        })).isInstanceOf(HttpClientErrorException.NotFound.class);
        assertThat(chamadas).hasValue(1);
    }
}
