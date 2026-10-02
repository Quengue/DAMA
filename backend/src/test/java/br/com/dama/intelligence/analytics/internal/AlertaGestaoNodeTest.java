package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.AlertaGestaoView;
import br.com.dama.intelligence.analytics.internal.AlertaGestaoClient.EntradaAnalise;
import br.com.dama.intelligence.analytics.internal.AlertaGestaoClient.Tendencia;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ColaboradorSemAtividade;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.MetaAberta;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ValorIndicador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** O alert-service é simulado por um servidor HTTP do JDK: valida o contrato e o fallback sem precisar de Docker. */
class AlertaGestaoNodeTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 9, 28);

    private HttpServer servidor;
    private final AtomicReference<String> corpoRecebido = new AtomicReference<>();
    private final AtomicReference<String> caminhoRecebido = new AtomicReference<>();

    @BeforeEach
    void subirServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.start();
    }

    @AfterEach
    void derrubarServidor() {
        try {
            servidor.stop(0); // o teste "servicoForaDoAr" já parou o servidor
        } catch (RuntimeException ignorado) {
            // nada a fazer
        }
    }

    private String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    private void responder(int status, String corpo) {
        servidor.createContext("/avaliar", troca -> {
            caminhoRecebido.set(troca.getRequestMethod() + " " + troca.getRequestURI().getPath());
            corpoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(status, bytes.length);
            troca.getResponseBody().write(bytes);
            troca.close();
        });
    }

    private static EntradaAnalise entrada() {
        MetaAberta vencida = new MetaAberta(1L, "Entregas no prazo", 10L, "Lucas", null, null, new BigDecimal("100"),
                new BigDecimal("40"), HOJE.minusDays(60), HOJE.minusDays(1), true);
        ValorIndicador anterior = new ValorIndicador(5L, "Produtividade", "%", true, 10L, "Lucas", null, null,
                HOJE.minusMonths(1), new BigDecimal("80"));
        ValorIndicador atual = new ValorIndicador(5L, "Produtividade", "%", true, 10L, "Lucas", null, null,
                HOJE, new BigDecimal("56"));
        return new EntradaAnalise(HOJE, List.of(vencida), List.of(new Tendencia(anterior, atual)), List.of(),
                List.of(new ColaboradorSemAtividade(7L, "Marina", 2L)));
    }

    @Test
    void enviaOsDadosNoContratoEUsaOsAlertasDevolvidosPeloNode() throws Exception {
        responder(200, """
                {"motor":"alert-service-node","alertas":[
                  {"tipo":"META_VENCIDA","severidade":"ALTA","titulo":"Meta vencida: Entregas no prazo",
                   "detalhe":"d","recomendacao":"r","colaboradorId":10,"departamentoId":null,"referenciaId":1}]}""");

        List<AlertaGestaoView> alertas = new AlertaGestaoNode(url(), Duration.ofSeconds(2), new AlertaGestaoLocal())
                .avaliar(entrada());

        // veio do Node (um único alerta), não das regras locais (que gerariam três)
        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).tipo()).isEqualTo("META_VENCIDA");
        assertThat(alertas.get(0).colaboradorId()).isEqualTo(10L);
        assertThat(alertas.get(0).departamentoId()).isNull();
        assertThat(alertas.get(0).referenciaId()).isEqualTo(1L);

        assertThat(caminhoRecebido.get()).isEqualTo("POST /avaliar");
        JsonNode enviado = new ObjectMapper().readTree(corpoRecebido.get());
        assertThat(enviado.get("dataReferencia").asText()).isEqualTo("2026-09-28");
        assertThat(enviado.get("metas").get(0).get("dataFim").asText()).isEqualTo("2026-09-27");
        assertThat(enviado.get("metas").get(0).get("valorAlvo").decimalValue()).isEqualByComparingTo("100");
        assertThat(enviado.get("tendencias").get(0).get("atual").get("valor").decimalValue()).isEqualByComparingTo("56");
        assertThat(enviado.get("colaboradoresSemAtividade").get(0).get("nome").asText()).isEqualTo("Marina");
    }

    @Test
    void quandoOServicoRespondeErroUsaAsRegrasLocais() {
        responder(500, "{\"status\":500}");

        List<AlertaGestaoView> alertas = new AlertaGestaoNode(url(), Duration.ofSeconds(2), new AlertaGestaoLocal())
                .avaliar(entrada());

        assertThat(alertas).extracting(AlertaGestaoView::tipo)
                .containsExactly("META_VENCIDA", "INDICADOR_EM_QUEDA", "SEM_ATIVIDADE_RECENTE");
    }

    @Test
    void respostaForaDoContratoUsaAsRegrasLocais() {
        responder(200, "{\"alertas\":[{\"tipo\":\"META_VENCIDA\",\"severidade\":\"URGENTE\"}]}");

        assertThat(new AlertaGestaoNode(url(), Duration.ofSeconds(2), new AlertaGestaoLocal()).avaliar(entrada()))
                .hasSize(3);
    }

    @Test
    void servicoForaDoArUsaAsRegrasLocais() {
        String urlDoServidorParado = url();
        servidor.stop(0);

        assertThat(new AlertaGestaoNode(urlDoServidorParado, Duration.ofMillis(500), new AlertaGestaoLocal())
                .avaliar(entrada())).hasSize(3);
    }

    @Test
    void motorLocalAplicaAsRegrasNaMesmaOrdemDoContrato() {
        assertThat(new AlertaGestaoLocal().avaliar(entrada())).extracting(AlertaGestaoView::tipo)
                .containsExactly("META_VENCIDA", "INDICADOR_EM_QUEDA", "SEM_ATIVIDADE_RECENTE");
    }
}
