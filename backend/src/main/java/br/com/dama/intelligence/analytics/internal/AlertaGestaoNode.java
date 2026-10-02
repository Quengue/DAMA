package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.AlertaGestaoView;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ColaboradorSemAtividade;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.Equipe;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.MetaAberta;
import br.com.dama.intelligence.analytics.internal.AnaliseGestaoRegras.ValorIndicador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chama o alert-service (Node.js) por REST síncrono: {@code POST /avaliar}. O backend envia os dados brutos
 * e o serviço devolve os alertas. Se o serviço estiver fora do ar, lento ou responder algo inválido, a análise
 * não falha: cai no {@code fallback} (regras locais em Java) e o motivo vai para o log.
 */
final class AlertaGestaoNode implements AlertaGestaoClient {

    private static final Logger log = LoggerFactory.getLogger(AlertaGestaoNode.class);
    private static final Set<String> SEVERIDADES = Set.of("ALTA", "MEDIA", "BAIXA");

    private final URI endpoint;
    private final Duration timeout;
    private final AlertaGestaoClient fallback;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    AlertaGestaoNode(String baseUrl, Duration timeout, AlertaGestaoClient fallback) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.endpoint = URI.create(base + "/avaliar");
        this.timeout = timeout;
        this.fallback = fallback;
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
    }

    @Override
    public List<AlertaGestaoView> avaliar(EntradaAnalise entrada) {
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(corpo(entrada))))
                    .build();
            HttpResponse<byte[]> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofByteArray());
            if (resposta.statusCode() != 200) {
                throw new IOException("alert-service respondeu HTTP " + resposta.statusCode());
            }
            List<AlertaGestaoView> alertas = ler(resposta.body());
            log.info("alert-service avaliou a análise de gestão: {} alerta(s)", alertas.size());
            return alertas;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return usarFallback(entrada, e);
        } catch (IOException | RuntimeException e) {
            return usarFallback(entrada, e);
        }
    }

    private List<AlertaGestaoView> usarFallback(EntradaAnalise entrada, Exception causa) {
        log.warn("alert-service indisponível ({}); usando as regras locais do backend.", causa.toString());
        return fallback.avaliar(entrada);
    }

    // ---------- contrato de entrada (ver alert-service/README.md) ----------

    private static Map<String, Object> corpo(EntradaAnalise entrada) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("dataReferencia", entrada.hoje().toString());
        corpo.put("metas", entrada.metas().stream().map(AlertaGestaoNode::meta).toList());
        corpo.put("tendencias", entrada.tendencias().stream().map(t -> {
            Map<String, Object> par = new LinkedHashMap<>();
            par.put("anterior", valor(t.anterior()));
            par.put("atual", valor(t.atual()));
            return par;
        }).toList());
        corpo.put("equipes", entrada.equipes().stream().map(AlertaGestaoNode::equipe).toList());
        corpo.put("colaboradoresSemAtividade", entrada.semAtividade().stream().map(AlertaGestaoNode::semAtividade).toList());
        return corpo;
    }

    private static Map<String, Object> meta(MetaAberta m) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("metaId", m.metaId());
        mapa.put("descricao", m.descricao());
        mapa.put("colaboradorId", m.colaboradorId());
        mapa.put("colaboradorNome", m.colaboradorNome());
        mapa.put("departamentoId", m.departamentoId());
        mapa.put("departamentoNome", m.departamentoNome());
        mapa.put("valorAlvo", m.valorAlvo());
        mapa.put("progressoAtual", m.progressoAtual());
        mapa.put("dataInicio", m.dataInicio().toString());
        mapa.put("dataFim", m.dataFim().toString());
        mapa.put("maiorMelhor", m.maiorMelhor());
        return mapa;
    }

    private static Map<String, Object> valor(ValorIndicador v) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("indicadorId", v.indicadorId());
        mapa.put("indicadorNome", v.indicadorNome());
        mapa.put("unidade", v.unidade());
        mapa.put("maiorMelhor", v.maiorMelhor());
        mapa.put("colaboradorId", v.colaboradorId());
        mapa.put("colaboradorNome", v.colaboradorNome());
        mapa.put("departamentoId", v.departamentoId());
        mapa.put("departamentoNome", v.departamentoNome());
        mapa.put("periodo", v.periodo().toString());
        mapa.put("valor", v.valor());
        return mapa;
    }

    private static Map<String, Object> equipe(Equipe e) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("departamentoId", e.departamentoId());
        mapa.put("nome", e.nome());
        mapa.put("colaboradoresAtivos", e.colaboradoresAtivos());
        mapa.put("pontosTotais", e.pontosTotais());
        return mapa;
    }

    private static Map<String, Object> semAtividade(ColaboradorSemAtividade c) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("colaboradorId", c.colaboradorId());
        mapa.put("nome", c.nome());
        mapa.put("departamentoId", c.departamentoId());
        return mapa;
    }

    // ---------- contrato de saída ----------

    private List<AlertaGestaoView> ler(byte[] corpo) throws IOException {
        JsonNode lista = json.readTree(corpo).path("alertas");
        if (!lista.isArray()) {
            throw new IOException("resposta do alert-service sem a lista \"alertas\"");
        }
        List<AlertaGestaoView> alertas = new ArrayList<>();
        for (JsonNode no : lista) {
            String tipo = texto(no, "tipo");
            String severidade = texto(no, "severidade");
            if (tipo == null || severidade == null || !SEVERIDADES.contains(severidade)) {
                throw new IOException("alerta inválido na resposta do alert-service: " + no);
            }
            alertas.add(new AlertaGestaoView(tipo, severidade, texto(no, "titulo"), texto(no, "detalhe"),
                    texto(no, "recomendacao"), id(no, "colaboradorId"), id(no, "departamentoId"), id(no, "referenciaId")));
        }
        return alertas;
    }

    private static String texto(JsonNode no, String campo) {
        return no.hasNonNull(campo) ? no.get(campo).asText() : null;
    }

    private static Long id(JsonNode no, String campo) {
        return no.hasNonNull(campo) ? no.get(campo).asLong() : null;
    }
}
