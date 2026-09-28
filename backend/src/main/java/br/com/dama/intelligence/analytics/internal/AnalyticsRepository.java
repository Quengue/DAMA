package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.IndicadorDepartamentoView;
import br.com.dama.intelligence.analytics.api.RankingColaboradorView;
import br.com.dama.intelligence.analytics.api.RankingDepartamentoView;
import br.com.dama.intelligence.analytics.api.VisaoConsolidadaView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lê exclusivamente das views de database/02_views.sql — nenhuma tabela é acessada diretamente. */
@Repository
class AnalyticsRepository {

    private final JdbcTemplate jdbc;

    AnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    boolean existeEmpresa(Long empresaId) {
        Integer n = jdbc.queryForObject("select count(*) from dama.empresa where empresa_id = ?", Integer.class, empresaId);
        return n != null && n > 0;
    }

    Optional<VisaoConsolidadaView> visaoConsolidada(Long empresaId) {
        return jdbc.query(
                "select empresa_id, colaboradores_ativos, total_departamentos, metas_em_andamento, " +
                        "metas_concluidas, pontos_totais_periodo from dama.vw_visao_consolidada_empresa where empresa_id = ?",
                (rs, i) -> new VisaoConsolidadaView(
                        rs.getLong("empresa_id"), rs.getLong("colaboradores_ativos"), rs.getLong("total_departamentos"),
                        rs.getLong("metas_em_andamento"), rs.getLong("metas_concluidas"), rs.getBigDecimal("pontos_totais_periodo")),
                empresaId).stream().findFirst();
    }

    List<RankingDepartamentoView> rankingDepartamentos(Long empresaId) {
        return jdbc.query(
                "select departamento_id, departamento_nome, colaboradores_ativos, pontos_totais, " +
                        "pontos_media_por_lancamento, posicao from dama.vw_ranking_departamento " +
                        "where empresa_id = ? order by posicao",
                (rs, i) -> new RankingDepartamentoView(
                        rs.getLong("departamento_id"), rs.getString("departamento_nome"), rs.getLong("colaboradores_ativos"),
                        rs.getBigDecimal("pontos_totais"), rs.getBigDecimal("pontos_media_por_lancamento"), rs.getLong("posicao")),
                empresaId);
    }

    List<RankingColaboradorView> rankingColaboradores(Long empresaId) {
        return jdbc.query(
                "select colaborador_id, nome, departamento_id, pontos_totais, posicao " +
                        "from dama.vw_ranking_colaborador where empresa_id = ? order by posicao",
                (rs, i) -> new RankingColaboradorView(
                        rs.getLong("colaborador_id"), rs.getString("nome"), rs.getLong("departamento_id"),
                        rs.getBigDecimal("pontos_totais"), rs.getLong("posicao")),
                empresaId);
    }

    List<IndicadorDepartamentoView> indicadoresPorDepartamento(Long empresaId, Long indicadorId) {
        String sql = "select departamento_id, departamento_nome, indicador_id, indicador_nome, unidade, " +
                "periodo_referencia, valor from dama.vw_indicador_departamento_atual where empresa_id = ?";
        org.springframework.jdbc.core.RowMapper<IndicadorDepartamentoView> mapper = (rs, i) -> new IndicadorDepartamentoView(
                rs.getLong("departamento_id"), rs.getString("departamento_nome"), rs.getLong("indicador_id"),
                rs.getString("indicador_nome"), rs.getString("unidade"),
                rs.getDate("periodo_referencia").toLocalDate(), rs.getBigDecimal("valor"));
        if (indicadorId == null) {
            return jdbc.query(sql + " order by indicador_nome, valor desc", mapper, empresaId);
        }
        return jdbc.query(sql + " and indicador_id = ? order by valor desc", mapper, empresaId, indicadorId);
    }
}
