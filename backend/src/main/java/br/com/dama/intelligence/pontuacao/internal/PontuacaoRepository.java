package br.com.dama.intelligence.pontuacao.internal;

import br.com.dama.intelligence.pontuacao.api.PontuacaoView;
import br.com.dama.intelligence.pontuacao.api.RegraPontuacaoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
class PontuacaoRepository {

    private static final String REGRA_COLS = "regra_id, empresa_id, atividade_id, nome, pontos, ativa";
    private static final String PONTUACAO_COLS = "pontuacao_id, colaborador_id, regra_id, pontos, descricao, registrado_em";

    private final JdbcTemplate jdbc;

    PontuacaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    RegraPontuacaoView inserirRegra(Long empresaId, Long atividadeId, String nome, BigDecimal pontos) {
        return jdbc.queryForObject(
                "insert into dama.regra_pontuacao (empresa_id, atividade_id, nome, pontos) values (?, ?, ?, ?) returning " + REGRA_COLS,
                PontuacaoRepository::mapearRegra, empresaId, atividadeId, nome, pontos);
    }

    List<RegraPontuacaoView> listarRegras(Long empresaId) {
        return jdbc.query("select " + REGRA_COLS + " from dama.regra_pontuacao where empresa_id = ? order by nome",
                PontuacaoRepository::mapearRegra, empresaId);
    }

    Optional<RegraPontuacaoView> buscarRegra(Long regraId) {
        return jdbc.query("select " + REGRA_COLS + " from dama.regra_pontuacao where regra_id = ?",
                PontuacaoRepository::mapearRegra, regraId).stream().findFirst();
    }

    void atualizarRegraAtiva(Long regraId, boolean ativa) {
        jdbc.update("update dama.regra_pontuacao set ativa = ? where regra_id = ?", ativa, regraId);
    }

    PontuacaoView inserirPontuacao(Long colaboradorId, Long regraId, BigDecimal pontos, String descricao) {
        return jdbc.queryForObject(
                "insert into dama.pontuacao (colaborador_id, regra_id, pontos, descricao) values (?, ?, ?, ?) returning " + PONTUACAO_COLS,
                PontuacaoRepository::mapearPontuacao, colaboradorId, regraId, pontos, descricao);
    }

    List<PontuacaoView> historico(Long colaboradorId) {
        return jdbc.query("select " + PONTUACAO_COLS + " from dama.pontuacao where colaborador_id = ? " +
                        "order by registrado_em desc, pontuacao_id desc",
                PontuacaoRepository::mapearPontuacao, colaboradorId);
    }

    BigDecimal total(Long colaboradorId) {
        BigDecimal total = jdbc.queryForObject(
                "select coalesce(sum(pontos), 0) from dama.pontuacao where colaborador_id = ?", BigDecimal.class, colaboradorId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private static RegraPontuacaoView mapearRegra(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RegraPontuacaoView(rs.getLong("regra_id"), rs.getLong("empresa_id"), rs.getObject("atividade_id", Long.class),
                rs.getString("nome"), rs.getBigDecimal("pontos"), rs.getBoolean("ativa"));
    }

    private static PontuacaoView mapearPontuacao(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new PontuacaoView(rs.getLong("pontuacao_id"), rs.getLong("colaborador_id"), rs.getObject("regra_id", Long.class),
                rs.getBigDecimal("pontos"), rs.getString("descricao"), rs.getTimestamp("registrado_em").toLocalDateTime());
    }
}
