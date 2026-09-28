package br.com.dama.intelligence.dashboard.internal;

import br.com.dama.intelligence.dashboard.api.IndicadorResumoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Somente leitura, direto sobre as tabelas de indicador/meta/pontuacao/ai_credit_transacao.
 * As tabelas de meta/pontuacao/ai_credit ainda não têm módulo de escrita próprio
 * (ver README — previsto para as próximas sprints, MVP 2 a 4), mas o dashboard já
 * pode consultá-las; indicador já tem módulo completo (ver pacote indicador).
 */
@Repository
class DashboardRepository {

    private final JdbcTemplate jdbc;

    DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<IndicadorResumoView> ultimosIndicadores(Long colaboradorId) {
        return jdbc.query(
                "select distinct on (iv.indicador_id) i.nome, i.unidade, iv.valor, iv.periodo_referencia " +
                        "from dama.indicador_valor iv join dama.indicador i on i.indicador_id = iv.indicador_id " +
                        "where iv.colaborador_id = ? " +
                        "order by iv.indicador_id, iv.periodo_referencia desc",
                (rs, i) -> new IndicadorResumoView(
                        rs.getString("nome"), rs.getString("unidade"),
                        rs.getBigDecimal("valor"), rs.getDate("periodo_referencia").toLocalDate()),
                colaboradorId);
    }

    long contarMetas(Long colaboradorId, String status) {
        Integer n = jdbc.queryForObject(
                "select count(*) from dama.meta where colaborador_id = ? and status = ?",
                Integer.class, colaboradorId, status);
        return n == null ? 0 : n;
    }

    BigDecimal pontosTotais(Long colaboradorId) {
        BigDecimal soma = jdbc.queryForObject(
                "select coalesce(sum(pontos), 0) from dama.pontuacao where colaborador_id = ?",
                BigDecimal.class, colaboradorId);
        return soma == null ? BigDecimal.ZERO : soma;
    }

    BigDecimal aiCreditsSaldo(Long colaboradorId) {
        BigDecimal saldo = jdbc.queryForObject(
                "select coalesce(sum(case when tipo = 'CREDITO' then quantidade else -quantidade end), 0) " +
                        "from dama.ai_credit_transacao where colaborador_id = ?",
                BigDecimal.class, colaboradorId);
        return saldo == null ? BigDecimal.ZERO : saldo;
    }
}
