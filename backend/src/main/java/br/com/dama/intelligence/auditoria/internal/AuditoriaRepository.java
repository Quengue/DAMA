package br.com.dama.intelligence.auditoria.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaEventoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
class AuditoriaRepository {

    private final JdbcTemplate jdbc;

    AuditoriaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    void inserir(Long empresaId, Long colaboradorId, String entidade, String entidadeId, String acao, String detalhesJson) {
        jdbc.update(
                "insert into dama.auditoria_evento (empresa_id, colaborador_id, entidade, entidade_id, acao, detalhes) " +
                        "values (?, ?, ?, ?, ?, cast(? as jsonb))",
                empresaId, colaboradorId, entidade, entidadeId, acao, detalhesJson);
    }

    List<AuditoriaEventoView> listar(Long empresaId, String entidade, String entidadeId) {
        StringBuilder sql = new StringBuilder(
                "select evento_id, colaborador_id, entidade, entidade_id, acao, detalhes, registrado_em " +
                        "from dama.auditoria_evento where empresa_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        if (entidade != null) {
            sql.append(" and entidade = ?");
            params.add(entidade);
        }
        if (entidadeId != null) {
            sql.append(" and entidade_id = ?");
            params.add(entidadeId);
        }
        sql.append(" order by registrado_em desc");

        return jdbc.query(sql.toString(), (rs, i) -> new AuditoriaEventoView(
                rs.getLong("evento_id"),
                rs.getObject("colaborador_id", Long.class),
                rs.getString("entidade"),
                rs.getString("entidade_id"),
                rs.getString("acao"),
                rs.getString("detalhes"),
                rs.getTimestamp("registrado_em").toLocalDateTime()
        ), params.toArray());
    }
}
