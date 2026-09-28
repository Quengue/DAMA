package br.com.dama.intelligence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a API inteira contra um PostgreSQL 16 criado com os scripts de database/ (schema, views e seed).
 * Cada teste cria a própria empresa para não depender dos demais. Pulado quando não há Docker disponível.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DamaApiIntegrationTest {

    private static final long ADMIN = 1L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("dama")
            .withCopyFileToContainer(MountableFile.forHostPath("../database/01_schema.sql"), "/docker-entrypoint-initdb.d/01_schema.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("../database/02_views.sql"), "/docker-entrypoint-initdb.d/02_views.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("../database/03_seed.sql"), "/docker-entrypoint-initdb.d/03_seed.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void visaoConsolidadaSomaPontosSemMultiplicarPorDepartamentosEMetas() {
        Empresa e = novaEmpresa();
        long regra = post(ADMIN, "/api/empresas/" + e.id + "/regras-pontuacao", Map.of("nome", "Entrega", "pontos", 10)).get("regraId").asLong();
        post(ADMIN, "/api/colaboradores/" + e.colaboradorA + "/pontuacao", Map.of("regraId", regra));
        post(ADMIN, "/api/colaboradores/" + e.colaboradorB + "/pontuacao", Map.of("regraId", regra));
        post(ADMIN, "/api/empresas/" + e.id + "/metas", meta(e.colaboradorA, "10"));
        post(ADMIN, "/api/empresas/" + e.id + "/metas", meta(e.colaboradorB, "10"));

        JsonNode visao = get(ADMIN, "/api/empresas/" + e.id + "/analytics/visao-consolidada");

        assertThat(visao.get("totalDepartamentos").asLong()).isEqualTo(2);
        assertThat(visao.get("metasEmAndamento").asLong()).isEqualTo(2);
        assertThat(visao.get("pontosTotaisPeriodo").decimalValue()).isEqualByComparingTo("20");
    }

    @Test
    void conquistaAutomaticaEhConcedidaAoAtingirOCriterioENivelSobe() {
        Empresa e = novaEmpresa();
        post(ADMIN, "/api/empresas/" + e.id + "/conquistas",
                Map.of("nome", "Cem pontos", "criterio", "PONTOS_TOTAIS", "valorMinimo", 100));
        long regra = post(ADMIN, "/api/empresas/" + e.id + "/regras-pontuacao", Map.of("nome", "Projeto", "pontos", 60)).get("regraId").asLong();

        post(ADMIN, "/api/colaboradores/" + e.colaboradorA + "/pontuacao", Map.of("regraId", regra));
        assertThat(get(ADMIN, "/api/colaboradores/" + e.colaboradorA + "/conquistas")).isEmpty();

        post(ADMIN, "/api/colaboradores/" + e.colaboradorA + "/pontuacao", Map.of("regraId", regra));
        JsonNode evolucao = get(ADMIN, "/api/colaboradores/" + e.colaboradorA + "/evolucao");

        assertThat(evolucao.get("conquistas")).hasSize(1);
        assertThat(evolucao.get("conquistas").get(0).get("nome").asText()).isEqualTo("Cem pontos");
        assertThat(evolucao.get("nivelAtual").get("nome").asText()).isEqualTo("Aprendiz");
        assertThat(evolucao.get("proximoNivel").get("nome").asText()).isEqualTo("Praticante");
    }

    @Test
    void conquistaCriadaDepoisVaiParaQuemJaCumpreOCriterio() {
        Empresa e = novaEmpresa();
        post(ADMIN, "/api/empresas/" + e.id + "/reconhecimentos",
                Map.of("colaboradorId", e.colaboradorB, "tipo", "Destaque", "descricao", "Entrega do trimestre"));

        JsonNode conquista = post(ADMIN, "/api/empresas/" + e.id + "/conquistas",
                Map.of("nome", "Reconhecido", "criterio", "RECONHECIMENTOS_RECEBIDOS", "valorMinimo", 1));

        assertThat(conquista.get("colaboradoresQueObtiveram").asLong()).isEqualTo(1);
        assertThat(get(ADMIN, "/api/colaboradores/" + e.colaboradorB + "/conquistas")).hasSize(1);
    }

    @Test
    void conquistaManualEhConcedidaUmaVezSo() {
        Empresa e = novaEmpresa();
        long conquista = post(ADMIN, "/api/empresas/" + e.id + "/conquistas",
                Map.of("nome", "Destaque do mês", "criterio", "MANUAL")).get("conquistaId").asLong();

        ResponseEntity<String> primeira = call(HttpMethod.POST, ADMIN, "/api/colaboradores/" + e.colaboradorA + "/conquistas",
                Map.of("conquistaId", conquista));
        ResponseEntity<String> repetida = call(HttpMethod.POST, ADMIN, "/api/colaboradores/" + e.colaboradorA + "/conquistas",
                Map.of("conquistaId", conquista));

        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void metaDeIndicadorMenorEhMelhorConcluiAoFicarAbaixoDoAlvo() {
        Empresa e = novaEmpresa();
        long indicador = post(ADMIN, "/api/empresas/" + e.id + "/indicadores",
                Map.of("nome", "Retrabalho", "unidade", "%", "maiorMelhor", false)).get("indicadorId").asLong();
        Map<String, Object> comando = meta(e.colaboradorA, "5");
        comando.put("indicadorId", indicador);
        long meta = post(ADMIN, "/api/empresas/" + e.id + "/metas", comando).get("metaId").asLong();

        post(ADMIN, "/api/metas/" + meta + "/progresso", Map.of("valorAtual", 4));

        assertThat(get(ADMIN, "/api/metas/" + meta).get("status").asText()).isEqualTo("Concluida");
    }

    @Test
    void alertasApontamMetaVencidaEIndicadorEmQueda() {
        Empresa e = novaEmpresa();
        Map<String, Object> vencida = meta(e.colaboradorA, "100");
        vencida.put("dataInicio", LocalDate.now().minusDays(40).toString());
        vencida.put("dataFim", LocalDate.now().minusDays(1).toString());
        post(ADMIN, "/api/empresas/" + e.id + "/metas", vencida);

        long indicador = post(ADMIN, "/api/empresas/" + e.id + "/indicadores",
                Map.of("nome", "Entregas no prazo", "unidade", "%")).get("indicadorId").asLong();
        post(ADMIN, "/api/indicadores/" + indicador + "/valores", Map.of("colaboradorId", e.colaboradorB,
                "periodoReferencia", LocalDate.now().withDayOfMonth(1).minusMonths(1).toString(), "valor", 90));
        post(ADMIN, "/api/indicadores/" + indicador + "/valores", Map.of("colaboradorId", e.colaboradorB,
                "periodoReferencia", LocalDate.now().withDayOfMonth(1).toString(), "valor", 60));

        JsonNode analise = get(ADMIN, "/api/empresas/" + e.id + "/analytics/alertas");

        assertThat(analise.get("alertas").findValuesAsText("tipo")).contains("META_VENCIDA", "INDICADOR_EM_QUEDA");
        assertThat(analise.get("alertas").get(0).get("severidade").asText()).isEqualTo("ALTA");
    }

    @Test
    void dashboardDaEquipeConsolidaMembros() {
        Empresa e = novaEmpresa();
        long regra = post(ADMIN, "/api/empresas/" + e.id + "/regras-pontuacao", Map.of("nome", "Entrega", "pontos", 15)).get("regraId").asLong();
        post(ADMIN, "/api/colaboradores/" + e.colaboradorA + "/pontuacao", Map.of("regraId", regra));

        JsonNode dashboard = get(ADMIN, "/api/departamentos/" + e.departamentoA + "/dashboard");

        assertThat(dashboard.get("colaboradoresAtivos").asLong()).isEqualTo(1);
        assertThat(dashboard.get("pontosTotais").decimalValue()).isEqualByComparingTo("15");
        assertThat(dashboard.get("posicaoNoRanking").asLong()).isEqualTo(1);
        assertThat(dashboard.get("membros").get(0).get("colaboradorId").asLong()).isEqualTo(e.colaboradorA);
    }

    @Test
    void perfilColaboradorVeRankingMasNaoVeAlertasNemAuditoria() {
        Empresa e = novaEmpresa();
        long perfilColaborador = 0;
        for (JsonNode perfil : get(ADMIN, "/api/perfis")) {
            if (perfil.get("nome").asText().equals("Colaborador")) {
                perfilColaborador = perfil.get("perfilId").asLong();
            }
        }
        call(HttpMethod.POST, ADMIN, "/api/colaboradores/" + e.colaboradorA + "/perfis", Map.of("perfilId", perfilColaborador));

        JsonNode sessao = get(e.colaboradorA, "/api/me");
        assertThat(sessao.get("perfis").get(0).asText()).isEqualTo("Colaborador");
        assertThat(sessao.get("permissoes").toString()).contains("GAMIFICACAO_LER").doesNotContain("ANALYTICS_LER");
        assertThat(call(HttpMethod.GET, e.colaboradorA, "/api/empresas/" + e.id + "/analytics/ranking-colaboradores", null)
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(call(HttpMethod.GET, e.colaboradorA, "/api/empresas/" + e.id + "/analytics/alertas", null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, e.colaboradorA, "/api/empresas/" + e.id + "/auditoria", null)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void requisicaoSemCabecalhoNaoEhAutenticada() {
        assertThat(call(HttpMethod.GET, null, "/api/me", null).getStatusCode().value()).isIn(401, 403);
    }

    // ---- helpers ------------------------------------------------------------------------------

    private record Empresa(long id, long departamentoA, long departamentoB, long colaboradorA, long colaboradorB) {
    }

    private Empresa novaEmpresa() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        long empresa = post(ADMIN, "/api/empresas", Map.of("nome", "Empresa " + sufixo)).get("empresaId").asLong();
        long depA = post(ADMIN, "/api/empresas/" + empresa + "/departamentos", Map.of("nome", "Tecnologia")).get("departamentoId").asLong();
        long depB = post(ADMIN, "/api/empresas/" + empresa + "/departamentos", Map.of("nome", "Comercial")).get("departamentoId").asLong();
        long colA = colaborador(empresa, depA, "ana." + sufixo);
        long colB = colaborador(empresa, depB, "bruno." + sufixo);
        return new Empresa(empresa, depA, depB, colA, colB);
    }

    private long colaborador(long empresa, long departamento, String login) {
        return post(ADMIN, "/api/empresas/" + empresa + "/colaboradores", Map.of("departamentoId", departamento,
                "nome", login, "email", login + "@teste.com", "dataAdmissao", "2025-01-10")).get("colaboradorId").asLong();
    }

    private static Map<String, Object> meta(long colaborador, String alvo) {
        Map<String, Object> comando = new java.util.HashMap<>();
        comando.put("colaboradorId", colaborador);
        comando.put("descricao", "Meta " + alvo);
        comando.put("valorAlvo", alvo);
        comando.put("dataInicio", LocalDate.now().minusDays(1).toString());
        comando.put("dataFim", LocalDate.now().plusDays(60).toString());
        return comando;
    }

    private JsonNode get(Long usuario, String path) {
        ResponseEntity<String> resposta = call(HttpMethod.GET, usuario, path, null);
        assertThat(resposta.getStatusCode().is2xxSuccessful()).as(path + " -> " + resposta.getBody()).isTrue();
        return parse(resposta.getBody());
    }

    private JsonNode post(Long usuario, String path, Object corpo) {
        ResponseEntity<String> resposta = call(HttpMethod.POST, usuario, path, corpo);
        assertThat(resposta.getStatusCode().is2xxSuccessful()).as(path + " -> " + resposta.getBody()).isTrue();
        return parse(resposta.getBody());
    }

    private ResponseEntity<String> call(HttpMethod metodo, Long usuario, String path, Object corpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (usuario != null) {
            headers.set("X-Colaborador-Id", usuario.toString());
        }
        return rest.exchange(path, metodo, new HttpEntity<>(corpo, headers), String.class);
    }

    private JsonNode parse(String corpo) {
        try {
            return corpo == null || corpo.isBlank() ? json.nullNode() : json.readTree(corpo);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
