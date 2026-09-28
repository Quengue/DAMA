package br.com.dama.intelligence.dashboard.internal;

import br.com.dama.intelligence.dashboard.api.IndicadorResumoView;
import br.com.dama.intelligence.dashboard.api.MembroEquipeView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Somente leitura, direto sobre as tabelas de origem (RNF04): nada do dashboard é armazenado. */
@Repository
class DashboardRepository {

    /** Metas da equipe = metas do departamento + metas individuais dos membros ativos. */
    private static final String METAS_DA_EQUIPE =
            "from dama.meta m where (m.departamento_id = ? or m.colaborador_id in " +
                    "(select colaborador_id from dama.colaborador where departamento_id = ? and ativo)) ";

    private static final String SALDO =
            "coalesce(sum(case when t.tipo = 'CREDITO' then t.quantidade else -t.quantidade end), 0)";

    private final JdbcTemplate jdbc;

    DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- colaborador --------------------------------------------------------------------------

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
        return contar("select count(*) from dama.meta where colaborador_id = ? and status = ?", colaboradorId, status);
    }

    BigDecimal pontosTotais(Long colaboradorId) {
        return somar("select coalesce(sum(pontos), 0) from dama.pontuacao where colaborador_id = ?", colaboradorId);
    }

    BigDecimal aiCreditsSaldo(Long colaboradorId) {
        return somar("select " + SALDO + " from dama.ai_credit_transacao t where t.colaborador_id = ?", colaboradorId);
    }

    // ---- equipe -------------------------------------------------------------------------------

    long colaboradoresAtivosDaEquipe(Long departamentoId) {
        return contar("select count(*) from dama.colaborador where departamento_id = ? and ativo", departamentoId);
    }

    List<IndicadorResumoView> ultimosIndicadoresDaEquipe(Long departamentoId) {
        return jdbc.query(
                "select indicador_nome, unidade, valor, periodo_referencia from dama.vw_indicador_departamento_atual " +
                        "where departamento_id = ? order by indicador_nome",
                (rs, i) -> new IndicadorResumoView(
                        rs.getString("indicador_nome"), rs.getString("unidade"),
                        rs.getBigDecimal("valor"), rs.getDate("periodo_referencia").toLocalDate()),
                departamentoId);
    }

    long contarMetasDaEquipe(Long departamentoId, String status) {
        return contar("select count(*) " + METAS_DA_EQUIPE + "and m.status = ?", departamentoId, departamentoId, status);
    }

    long contarMetasVencidasDaEquipe(Long departamentoId, LocalDate hoje) {
        return contar("select count(*) " + METAS_DA_EQUIPE + "and m.status in ('Em andamento', 'Atrasada') and m.data_fim < ?",
                departamentoId, departamentoId, hoje);
    }

    BigDecimal pontosDaEquipe(Long departamentoId) {
        return somar("select coalesce(sum(p.pontos), 0) from dama.pontuacao p " +
                "join dama.colaborador c on c.colaborador_id = p.colaborador_id " +
                "where c.departamento_id = ? and c.ativo", departamentoId);
    }

    BigDecimal aiCreditsSaldoDaEquipe(Long departamentoId) {
        return somar("select " + SALDO + " from dama.ai_credit_transacao t " +
                "join dama.colaborador c on c.colaborador_id = t.colaborador_id " +
                "where c.departamento_id = ? and c.ativo", departamentoId);
    }

    long conquistasDaEquipe(Long departamentoId) {
        return contar("select count(*) from dama.colaborador_conquista cc " +
                "join dama.colaborador c on c.colaborador_id = cc.colaborador_id " +
                "where c.departamento_id = ? and c.ativo", departamentoId);
    }

    Long posicaoNoRanking(Long departamentoId) {
        return jdbc.query("select posicao from dama.vw_ranking_departamento where departamento_id = ?",
                (rs, i) -> rs.getLong("posicao"), departamentoId).stream().findFirst().orElse(null);
    }

    List<MembroEquipeView> membros(Long departamentoId) {
        return jdbc.query(
                "select c.colaborador_id, c.nome, c.cargo, " +
                        "(select coalesce(sum(p.pontos), 0) from dama.pontuacao p where p.colaborador_id = c.colaborador_id) as pontos, " +
                        "(select " + SALDO + " from dama.ai_credit_transacao t where t.colaborador_id = c.colaborador_id) as saldo, " +
                        "(select count(*) from dama.colaborador_conquista cc where cc.colaborador_id = c.colaborador_id) as conquistas " +
                        "from dama.colaborador c where c.departamento_id = ? and c.ativo " +
                        "order by pontos desc, c.nome",
                (rs, i) -> new MembroEquipeView(rs.getLong("colaborador_id"), rs.getString("nome"), rs.getString("cargo"),
                        rs.getBigDecimal("pontos"), rs.getBigDecimal("saldo"), rs.getLong("conquistas")),
                departamentoId);
    }

    // ---- helpers ------------------------------------------------------------------------------

    private long contar(String sql, Object... params) {
        Long n = jdbc.queryForObject(sql, Long.class, params);
        return n == null ? 0 : n;
    }

    private BigDecimal somar(String sql, Object... params) {
        BigDecimal soma = jdbc.queryForObject(sql, BigDecimal.class, params);
        return soma == null ? BigDecimal.ZERO : soma;
    }
}
