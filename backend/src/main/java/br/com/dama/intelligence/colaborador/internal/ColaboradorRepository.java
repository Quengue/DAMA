package br.com.dama.intelligence.colaborador.internal;

import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class ColaboradorRepository {

    private static final String SELECT_BASE =
            "select c.colaborador_id, c.empresa_id, c.departamento_id, d.nome as departamento_nome, " +
                    "c.nome, c.email, c.cargo, c.senioridade, c.data_admissao, c.ativo " +
                    "from dama.colaborador c join dama.departamento d on d.departamento_id = c.departamento_id ";

    private final JdbcTemplate jdbc;

    ColaboradorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    boolean existeEmpresa(Long empresaId) {
        Integer n = jdbc.queryForObject("select count(*) from dama.empresa where empresa_id = ?", Integer.class, empresaId);
        return n != null && n > 0;
    }

    boolean departamentoPertenceAEmpresa(Long departamentoId, Long empresaId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from dama.departamento where departamento_id = ? and empresa_id = ?",
                Integer.class, departamentoId, empresaId);
        return n != null && n > 0;
    }

    boolean emailJaCadastrado(Long empresaId, String email, Long ignorarColaboradorId) {
        String sql = "select count(*) from dama.colaborador where empresa_id = ? and lower(email) = lower(?)";
        Integer n;
        if (ignorarColaboradorId == null) {
            n = jdbc.queryForObject(sql, Integer.class, empresaId, email);
        } else {
            n = jdbc.queryForObject(sql + " and colaborador_id <> ?", Integer.class, empresaId, email, ignorarColaboradorId);
        }
        return n != null && n > 0;
    }

    boolean existe(Long colaboradorId) {
        Integer n = jdbc.queryForObject("select count(*) from dama.colaborador where colaborador_id = ?", Integer.class, colaboradorId);
        return n != null && n > 0;
    }

    Long inserir(Long empresaId, Long departamentoId, String nome, String email, String cargo,
                 String senioridade, java.time.LocalDate dataAdmissao) {
        return jdbc.queryForObject(
                "insert into dama.colaborador (empresa_id, departamento_id, nome, email, cargo, senioridade, data_admissao) " +
                        "values (?, ?, ?, ?, ?, ?, ?) returning colaborador_id",
                Long.class, empresaId, departamentoId, nome, email, cargo, senioridade, dataAdmissao);
    }

    void atualizar(Long colaboradorId, Long departamentoId, String nome, String email, String cargo, String senioridade) {
        jdbc.update(
                "update dama.colaborador set departamento_id = ?, nome = ?, email = ?, cargo = ?, senioridade = ?, " +
                        "atualizado_em = now() where colaborador_id = ?",
                departamentoId, nome, email, cargo, senioridade, colaboradorId);
    }

    void alterarStatus(Long colaboradorId, boolean ativo) {
        jdbc.update("update dama.colaborador set ativo = ?, atualizado_em = now() where colaborador_id = ?", ativo, colaboradorId);
    }

    List<ColaboradorView> listarPorEmpresa(Long empresaId) {
        return jdbc.query(SELECT_BASE + "where c.empresa_id = ? order by c.nome", ColaboradorRepository::mapear, empresaId);
    }

    Optional<ColaboradorView> buscarPorId(Long colaboradorId) {
        return jdbc.query(SELECT_BASE + "where c.colaborador_id = ?", ColaboradorRepository::mapear, colaboradorId)
                .stream().findFirst();
    }

    private static ColaboradorView mapear(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ColaboradorView(
                rs.getLong("colaborador_id"),
                rs.getLong("empresa_id"),
                rs.getLong("departamento_id"),
                rs.getString("departamento_nome"),
                rs.getString("nome"),
                rs.getString("email"),
                rs.getString("cargo"),
                rs.getString("senioridade"),
                rs.getDate("data_admissao").toLocalDate(),
                rs.getBoolean("ativo")
        );
    }
}
