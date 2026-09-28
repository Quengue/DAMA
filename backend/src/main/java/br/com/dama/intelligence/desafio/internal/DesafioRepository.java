package br.com.dama.intelligence.desafio.internal;

import br.com.dama.intelligence.desafio.api.DesafioView;
import br.com.dama.intelligence.desafio.api.ParticipanteView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
class DesafioRepository {

    private static final String SELECT_BASE =
            "select d.desafio_id, d.empresa_id, d.nome, d.descricao, d.data_inicio, d.data_fim, d.pontos_recompensa, d.status, " +
                    "(select count(*) from dama.desafio_participante p where p.desafio_id = d.desafio_id) as total_participantes " +
                    "from dama.desafio d ";
    private static final String PARTICIPANTE_COLS = "desafio_id, colaborador_id, status, pontos_obtidos";

    private final JdbcTemplate jdbc;

    DesafioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Long inserir(Long empresaId, String nome, String descricao, LocalDate dataInicio, LocalDate dataFim, BigDecimal pontos) {
        return jdbc.queryForObject(
                "insert into dama.desafio (empresa_id, nome, descricao, data_inicio, data_fim, pontos_recompensa) " +
                        "values (?, ?, ?, ?, ?, ?) returning desafio_id",
                Long.class, empresaId, nome, descricao, dataInicio, dataFim, pontos);
    }

    Optional<DesafioView> buscar(Long desafioId) {
        return jdbc.query(SELECT_BASE + "where d.desafio_id = ?", DesafioRepository::mapearDesafio, desafioId)
                .stream().findFirst();
    }

    List<DesafioView> listar(Long empresaId, String status) {
        StringBuilder sql = new StringBuilder(SELECT_BASE + "where d.empresa_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        if (status != null) {
            sql.append(" and d.status = ?");
            params.add(status);
        }
        sql.append(" order by d.data_inicio desc, d.desafio_id desc");
        return jdbc.query(sql.toString(), DesafioRepository::mapearDesafio, params.toArray());
    }

    void atualizarStatus(Long desafioId, String status) {
        jdbc.update("update dama.desafio set status = ? where desafio_id = ?", status, desafioId);
    }

    Optional<ParticipanteView> buscarParticipante(Long desafioId, Long colaboradorId) {
        return jdbc.query("select " + PARTICIPANTE_COLS + " from dama.desafio_participante where desafio_id = ? and colaborador_id = ?",
                DesafioRepository::mapearParticipante, desafioId, colaboradorId).stream().findFirst();
    }

    ParticipanteView inserirParticipante(Long desafioId, Long colaboradorId) {
        return jdbc.queryForObject(
                "insert into dama.desafio_participante (desafio_id, colaborador_id) values (?, ?) returning " + PARTICIPANTE_COLS,
                DesafioRepository::mapearParticipante, desafioId, colaboradorId);
    }

    List<ParticipanteView> listarParticipantes(Long desafioId) {
        return jdbc.query("select " + PARTICIPANTE_COLS + " from dama.desafio_participante where desafio_id = ? order by colaborador_id",
                DesafioRepository::mapearParticipante, desafioId);
    }

    void atualizarParticipante(Long desafioId, Long colaboradorId, String status, BigDecimal pontosObtidos) {
        jdbc.update("update dama.desafio_participante set status = ?, pontos_obtidos = ? where desafio_id = ? and colaborador_id = ?",
                status, pontosObtidos, desafioId, colaboradorId);
    }

    private static DesafioView mapearDesafio(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new DesafioView(rs.getLong("desafio_id"), rs.getLong("empresa_id"), rs.getString("nome"), rs.getString("descricao"),
                rs.getDate("data_inicio").toLocalDate(), rs.getDate("data_fim").toLocalDate(),
                rs.getBigDecimal("pontos_recompensa"), rs.getString("status"), rs.getLong("total_participantes"));
    }

    private static ParticipanteView mapearParticipante(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ParticipanteView(rs.getLong("desafio_id"), rs.getLong("colaborador_id"),
                rs.getString("status"), rs.getBigDecimal("pontos_obtidos"));
    }
}
