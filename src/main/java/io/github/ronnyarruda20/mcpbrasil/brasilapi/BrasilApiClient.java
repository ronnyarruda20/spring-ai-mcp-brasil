package io.github.ronnyarruda20.mcpbrasil.brasilapi;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Atividade;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Empresa;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Endereco;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Feriado;
import io.github.ronnyarruda20.mcpbrasil.brasilapi.Modelos.Socio;
import io.github.ronnyarruda20.mcpbrasil.http.ConsultaException;
import io.github.ronnyarruda20.mcpbrasil.http.Tentativas;

/** Cliente da BrasilAPI (brasilapi.com.br): CNPJ, CEP e feriados nacionais. */
public class BrasilApiClient {

    private final RestClient http;
    private final Tentativas tentativas;

    public BrasilApiClient(RestClient http, Tentativas tentativas) {
        this.http = http;
        this.tentativas = tentativas;
    }

    public Empresa cnpj(String cnpj) {
        var r = chamar("CNPJ " + cnpj, "CNPJ " + cnpj + " não encontrado na base da Receita Federal.",
                () -> http.get().uri("/cnpj/v1/{cnpj}", cnpj).retrieve().body(Modelos.CnpjBrasilApi.class));
        return new Empresa(
                r.cnpj(),
                r.razaoSocial(),
                vazioParaNulo(r.nomeFantasia()),
                r.situacaoCadastral(),
                r.dataSituacaoCadastral(),
                r.dataInicioAtividade(),
                r.matrizFilial(),
                r.naturezaJuridica(),
                r.porte(),
                r.capitalSocial(),
                new Atividade(texto(r.cnaeFiscal()), r.cnaeFiscalDescricao()),
                r.cnaesSecundarios() == null ? List.of()
                        : r.cnaesSecundarios().stream()
                                .filter(c -> c.codigo() != null && !"0".equals(texto(c.codigo())))
                                .map(c -> new Atividade(texto(c.codigo()), c.descricao()))
                                .toList(),
                endereco(r),
                r.municipio(),
                r.uf(),
                r.cep(),
                r.opcaoPeloSimples(),
                r.opcaoPeloMei(),
                r.qsa() == null ? List.of()
                        : r.qsa().stream().map(s -> new Socio(s.nome(), s.qualificacao(), s.dataEntrada())).toList());
    }

    public Endereco cep(String cep) {
        var r = chamar("CEP " + cep, "CEP " + cep + " não encontrado.",
                () -> http.get().uri("/cep/v2/{cep}", cep).retrieve().body(Modelos.CepBrasilApi.class));
        var coord = r.location() == null ? null : r.location().coordinates();
        return new Endereco(
                r.cep(),
                vazioParaNulo(r.street()),
                vazioParaNulo(r.neighborhood()),
                r.city(),
                r.state(),
                r.ibge() == null ? null : r.ibge().city(),
                coord == null ? null : vazioParaNulo(coord.latitude()),
                coord == null ? null : vazioParaNulo(coord.longitude()),
                r.service());
    }

    public List<Feriado> feriados(int ano) {
        List<Modelos.FeriadoBrasilApi> r = chamar("feriados de " + ano, "Não há feriados cadastrados para " + ano + ".",
                () -> http.get().uri("/feriados/v1/{ano}", ano).retrieve()
                        .body(new ParameterizedTypeReference<List<Modelos.FeriadoBrasilApi>>() {
                        }));
        return r.stream().map(BrasilApiClient::feriado).toList();
    }

    static Feriado feriado(Modelos.FeriadoBrasilApi f) {
        boolean fimDeSemana = false;
        if (f.date() != null) {
            var dia = LocalDate.parse(f.date()).getDayOfWeek();
            fimDeSemana = dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
        }
        return new Feriado(f.date(), f.name(), f.weekday(), TipoFeriado.classificar(f.name()).descricao(), fimDeSemana);
    }

    private <T> T chamar(String oQue, String naoEncontrado, Supplier<T> chamada) {
        T resposta;
        try {
            resposta = tentativas.executar(chamada);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ConsultaException(naoEncontrado, e);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new ConsultaException("A BrasilAPI recusou a consulta de " + oQue + ": " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new ConsultaException("Não foi possível consultar " + oQue + " na BrasilAPI: " + e.getMessage(), e);
        }
        if (resposta == null) {
            throw new ConsultaException(naoEncontrado);
        }
        return resposta;
    }

    private static String endereco(Modelos.CnpjBrasilApi r) {
        String rua = Stream.of(r.tipoLogradouro(), r.logradouro())
                .map(BrasilApiClient::vazioParaNulo).filter(Objects::nonNull).collect(Collectors.joining(" "));
        return Stream.of(rua, vazioParaNulo(r.numero()), vazioParaNulo(r.complemento()), vazioParaNulo(r.bairro()))
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private static String texto(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String vazioParaNulo(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
