package br.com.dama.intelligence.aicredit.internal;

import br.com.dama.intelligence.aicredit.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
class AiCreditRepository {

    private static final String ATIVIDADE_COLS = "atividade_id, empresa_id, nome, descricao, ativa";
    private static final String REGRA_COLS =
            "regra_id, empresa_id, atividade_id, nome, criterio_elegibilidade, creditos_concedidos, ativa";
    private static final String TRANSACAO_COLS =
            "transacao_id, colaborador_id, regra_id, registro_id, tipo, quantidade, motivo, registrado_em";
    private static final String REGISTRO_COLS = "registro_id, atividade_id, colaborador_id, quantidade, registrado_em";

    private final JdbcTemplate jdbc;

    AiCreditRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- atividades produtivas ---------------------------------------------------------------

    AtividadeProdutivaView inserirAtividade(Long empresaId, String nome, String descricao) {
        return jdbc.queryForObject(
                "insert into dama.atividade_produtiva (empresa_id, nome, descricao) values (?, ?, ?) returning " + ATIVIDADE_COLS,
                AiCreditRepository::mapearAtividade, empresaId, nome, descricao);
    }

    List<AtividadeProdutivaView> listarAtividades(Long empresaId) {
        return jdbc.query("select " + ATIVIDADE_COLS + " from dama.atividade_produtiva where empresa_id = ? order by nome",
                AiCreditRepository::mapearAtividade, empresaId);
    }

    Optional<AtividadeProdutivaView> buscarAtividade(Long atividadeId) {
        return jdbc.query("select " + ATIVIDADE_COLS + " from dama.atividade_produtiva where atividade_id = ?",
                AiCreditRepository::mapearAtividade, atividadeId).stream().findFirst();
    }

    // ---- registros de atividade --------------------------------------------------------------

    AtividadeRegistroView inserirRegistro(Long atividadeId, Long colaboradorId, BigDecimal quantidade) {
        return jdbc.queryForObject(
                "insert into dama.atividade_registro (atividade_id, colaborador_id, quantidade) values (?, ?, ?) returning " + REGISTRO_COLS,
                AiCreditRepository::mapearRegistro, atividadeId, colaboradorId, quantidade);
    }

    List<AtividadeRegistroView> listarRegistros(Long colaboradorId) {
        return jdbc.query("select " + REGISTRO_COLS + " from dama.atividade_registro where colaborador_id = ? " +
                        "order by registrado_em desc, registro_id desc",
                AiCreditRepository::mapearRegistro, colaboradorId);
    }

    // ---- regras de crédito -------------------------------------------------------------------

    RegraCreditoView inserirRegra(Long empresaId, Long atividadeId, String nome, String criterio, BigDecimal creditos) {
        return jdbc.queryForObject(
                "insert into dama.ai_credit_regra (empresa_id, atividade_id, nome, criterio_elegibilidade, creditos_concedidos) " +
                        "values (?, ?, ?, ?, ?) returning " + REGRA_COLS,
                AiCreditRepository::mapearRegra, empresaId, atividadeId, nome, criterio, creditos);
    }

    List<RegraCreditoView> listarRegras(Long empresaId) {
        return jdbc.query("select " + REGRA_COLS + " from dama.ai_credit_regra where empresa_id = ? order by nome",
                AiCreditRepository::mapearRegra, empresaId);
    }

    List<RegraCreditoView> regrasAtivasDaAtividade(Long atividadeId) {
        return jdbc.query("select " + REGRA_COLS + " from dama.ai_credit_regra where atividade_id = ? and ativa order by regra_id",
                AiCreditRepository::mapearRegra, atividadeId);
    }

    // ---- transações e saldo ------------------------------------------------------------------

    /**
     * Serializa operações de saldo de um mesmo colaborador até o fim da transação corrente,
     * evitando que dois débitos concorrentes ultrapassem o saldo disponível.
     */
    void bloquearSaldo(Long colaboradorId) {
        jdbc.query("select pg_advisory_xact_lock(?)", (RowCallbackHandler) rs -> { }, colaboradorId);
    }

    TransacaoCreditoView inserirTransacao(Long colaboradorId, Long regraId, Long registroId, String tipo,
                                          BigDecimal quantidade, String motivo) {
        return jdbc.queryForObject(
                "insert into dama.ai_credit_transacao (colaborador_id, regra_id, registro_id, tipo, quantidade, motivo) " +
                        "values (?, ?, ?, ?, ?, ?) returning " + TRANSACAO_COLS,
                AiCreditRepository::mapearTransacao, colaboradorId, regraId, registroId, tipo, quantidade, motivo);
    }

    List<TransacaoCreditoView> historico(Long colaboradorId) {
        return jdbc.query("select " + TRANSACAO_COLS + " from dama.ai_credit_transacao where colaborador_id = ? " +
                        "order by registrado_em desc, transacao_id desc",
                AiCreditRepository::mapearTransacao, colaboradorId);
    }

    BigDecimal saldo(Long colaboradorId) {
        BigDecimal saldo = jdbc.queryForObject(
                "select coalesce(sum(case when tipo = 'CREDITO' then quantidade else -quantidade end), 0) " +
                        "from dama.ai_credit_transacao where colaborador_id = ?",
                BigDecimal.class, colaboradorId);
        return saldo == null ? BigDecimal.ZERO : saldo;
    }

    // ---- níveis de autonomia -----------------------------------------------------------------

    NivelAutonomiaView inserirNivel(String nome, BigDecimal creditosMinimos, int ordem, String descricao) {
        return jdbc.queryForObject(
                "insert into dama.ai_nivel_autonomia (nome, creditos_minimos, ordem, descricao) values (?, ?, ?, ?) " +
                        "returning nivel_id, nome, creditos_minimos, ordem, descricao",
                AiCreditRepository::mapearNivel, nome, creditosMinimos, ordem, descricao);
    }

    List<NivelAutonomiaView> listarNiveis() {
        return jdbc.query("select nivel_id, nome, creditos_minimos, ordem, descricao from dama.ai_nivel_autonomia order by ordem",
                AiCreditRepository::mapearNivel);
    }

    /** Maior nível cujo saldo mínimo já foi atingido. */
    Optional<NivelAutonomiaView> nivelParaSaldo(BigDecimal saldo) {
        return jdbc.query("select nivel_id, nome, creditos_minimos, ordem, descricao from dama.ai_nivel_autonomia " +
                        "where creditos_minimos <= ? order by ordem desc limit 1",
                AiCreditRepository::mapearNivel, saldo).stream().findFirst();
    }

    // ---- mapeadores --------------------------------------------------------------------------

    private static AtividadeProdutivaView mapearAtividade(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AtividadeProdutivaView(rs.getLong("atividade_id"), rs.getLong("empresa_id"),
                rs.getString("nome"), rs.getString("descricao"), rs.getBoolean("ativa"));
    }

    private static AtividadeRegistroView mapearRegistro(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new AtividadeRegistroView(rs.getLong("registro_id"), rs.getLong("atividade_id"), rs.getLong("colaborador_id"),
                rs.getBigDecimal("quantidade"), rs.getTimestamp("registrado_em").toLocalDateTime());
    }

    private static RegraCreditoView mapearRegra(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RegraCreditoView(rs.getLong("regra_id"), rs.getLong("empresa_id"), rs.getObject("atividade_id", Long.class),
                rs.getString("nome"), rs.getString("criterio_elegibilidade"), rs.getBigDecimal("creditos_concedidos"),
                rs.getBoolean("ativa"));
    }

    private static TransacaoCreditoView mapearTransacao(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new TransacaoCreditoView(rs.getLong("transacao_id"), rs.getLong("colaborador_id"),
                rs.getObject("regra_id", Long.class), rs.getObject("registro_id", Long.class),
                rs.getString("tipo"), rs.getBigDecimal("quantidade"), rs.getString("motivo"),
                rs.getTimestamp("registrado_em").toLocalDateTime());
    }

    private static NivelAutonomiaView mapearNivel(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new NivelAutonomiaView(rs.getLong("nivel_id"), rs.getString("nome"), rs.getBigDecimal("creditos_minimos"),
                rs.getInt("ordem"), rs.getString("descricao"));
    }
}
