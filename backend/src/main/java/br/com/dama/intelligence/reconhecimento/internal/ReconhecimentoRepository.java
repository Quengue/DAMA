package br.com.dama.intelligence.reconhecimento.internal;

import br.com.dama.intelligence.reconhecimento.api.ReconhecimentoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class ReconhecimentoRepository {

    private static final String COLS =
            "reconhecimento_id, empresa_id, colaborador_id, concedido_por_id, tipo, descricao, registrado_em";

    private final JdbcTemplate jdbc;

    ReconhecimentoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    ReconhecimentoView inserir(Long empresaId, Long colaboradorId, Long concedidoPorId, String tipo, String descricao) {
        return jdbc.queryForObject(
                "insert into dama.reconhecimento (empresa_id, colaborador_id, concedido_por_id, tipo, descricao) " +
                        "values (?, ?, ?, ?, ?) returning " + COLS,
                ReconhecimentoRepository::mapear, empresaId, colaboradorId, concedidoPorId, tipo, descricao);
    }

    List<ReconhecimentoView> listarPorEmpresa(Long empresaId) {
        return jdbc.query("select " + COLS + " from dama.reconhecimento where empresa_id = ? " +
                        "order by registrado_em desc, reconhecimento_id desc",
                ReconhecimentoRepository::mapear, empresaId);
    }

    List<ReconhecimentoView> listarDoColaborador(Long colaboradorId) {
        return jdbc.query("select " + COLS + " from dama.reconhecimento where colaborador_id = ? " +
                        "order by registrado_em desc, reconhecimento_id desc",
                ReconhecimentoRepository::mapear, colaboradorId);
    }

    private static ReconhecimentoView mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ReconhecimentoView(rs.getLong("reconhecimento_id"), rs.getLong("empresa_id"), rs.getLong("colaborador_id"),
                rs.getObject("concedido_por_id", Long.class), rs.getString("tipo"), rs.getString("descricao"),
                rs.getTimestamp("registrado_em").toLocalDateTime());
    }
}
