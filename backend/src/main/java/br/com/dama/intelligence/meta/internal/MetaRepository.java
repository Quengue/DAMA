package br.com.dama.intelligence.meta.internal;

import br.com.dama.intelligence.meta.api.MetaProgressoView;
import br.com.dama.intelligence.meta.api.MetaView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
class MetaRepository {

    private static final String SELECT_BASE =
            "select m.meta_id, m.empresa_id, m.indicador_id, m.colaborador_id, m.departamento_id, m.descricao, " +
                    "m.valor_alvo, m.data_inicio, m.data_fim, m.status, " +
                    "(select p.valor_atual from dama.meta_progresso p where p.meta_id = m.meta_id " +
                    " order by p.registrado_em desc, p.meta_progresso_id desc limit 1) as progresso_atual " +
                    "from dama.meta m ";

    private final JdbcTemplate jdbc;

    MetaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Long inserir(Long empresaId, Long indicadorId, Long colaboradorId, Long departamentoId, String descricao,
                 BigDecimal valorAlvo, LocalDate dataInicio, LocalDate dataFim) {
        return jdbc.queryForObject(
                "insert into dama.meta (empresa_id, indicador_id, colaborador_id, departamento_id, descricao, " +
                        "valor_alvo, data_inicio, data_fim) values (?, ?, ?, ?, ?, ?, ?, ?) returning meta_id",
                Long.class, empresaId, indicadorId, colaboradorId, departamentoId, descricao, valorAlvo, dataInicio, dataFim);
    }

    Optional<MetaView> buscar(Long metaId) {
        return jdbc.query(SELECT_BASE + "where m.meta_id = ?", MetaRepository::mapearMeta, metaId).stream().findFirst();
    }

    List<MetaView> listar(Long empresaId, Long colaboradorId, Long departamentoId, String status) {
        StringBuilder sql = new StringBuilder(SELECT_BASE + "where m.empresa_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        if (colaboradorId != null) {
            sql.append(" and m.colaborador_id = ?");
            params.add(colaboradorId);
        }
        if (departamentoId != null) {
            sql.append(" and m.departamento_id = ?");
            params.add(departamentoId);
        }
        if (status != null) {
            sql.append(" and m.status = ?");
            params.add(status);
        }
        sql.append(" order by m.data_fim, m.meta_id");
        return jdbc.query(sql.toString(), MetaRepository::mapearMeta, params.toArray());
    }

    void atualizarStatus(Long metaId, String status) {
        jdbc.update("update dama.meta set status = ? where meta_id = ?", status, metaId);
    }

    MetaProgressoView inserirProgresso(Long metaId, BigDecimal valorAtual) {
        return jdbc.queryForObject(
                "insert into dama.meta_progresso (meta_id, valor_atual) values (?, ?) " +
                        "returning meta_progresso_id, meta_id, valor_atual, registrado_em",
                MetaRepository::mapearProgresso, metaId, valorAtual);
    }

    List<MetaProgressoView> historico(Long metaId) {
        return jdbc.query(
                "select meta_progresso_id, meta_id, valor_atual, registrado_em from dama.meta_progresso " +
                        "where meta_id = ? order by registrado_em, meta_progresso_id",
                MetaRepository::mapearProgresso, metaId);
    }

    private static MetaView mapearMeta(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new MetaView(
                rs.getLong("meta_id"),
                rs.getLong("empresa_id"),
                rs.getObject("indicador_id", Long.class),
                rs.getObject("colaborador_id", Long.class),
                rs.getObject("departamento_id", Long.class),
                rs.getString("descricao"),
                rs.getBigDecimal("valor_alvo"),
                rs.getDate("data_inicio").toLocalDate(),
                rs.getDate("data_fim").toLocalDate(),
                rs.getString("status"),
                rs.getBigDecimal("progresso_atual")
        );
    }

    private static MetaProgressoView mapearProgresso(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new MetaProgressoView(
                rs.getLong("meta_progresso_id"),
                rs.getLong("meta_id"),
                rs.getBigDecimal("valor_atual"),
                rs.getTimestamp("registrado_em").toLocalDateTime()
        );
    }
}
