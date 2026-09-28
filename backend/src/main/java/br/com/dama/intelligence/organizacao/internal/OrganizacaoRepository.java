package br.com.dama.intelligence.organizacao.internal;

import br.com.dama.intelligence.organizacao.api.DepartamentoView;
import br.com.dama.intelligence.organizacao.api.EmpresaView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class OrganizacaoRepository {

    private final JdbcTemplate jdbc;

    OrganizacaoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    boolean existeEmpresa(Long empresaId) {
        Integer n = jdbc.queryForObject("select count(*) from dama.empresa where empresa_id = ?", Integer.class, empresaId);
        return n != null && n > 0;
    }

    Long inserirEmpresa(String nome, String setor, String porte) {
        return jdbc.queryForObject(
                "insert into dama.empresa (nome, setor, porte) values (?, ?, ?) returning empresa_id",
                Long.class, nome, setor, porte);
    }

    List<EmpresaView> listarEmpresas() {
        return jdbc.query("select empresa_id, nome, setor, porte from dama.empresa order by nome",
                (rs, i) -> new EmpresaView(rs.getLong("empresa_id"), rs.getString("nome"), rs.getString("setor"), rs.getString("porte")));
    }

    Long inserirDepartamento(Long empresaId, String nome) {
        return jdbc.queryForObject(
                "insert into dama.departamento (empresa_id, nome) values (?, ?) returning departamento_id",
                Long.class, empresaId, nome);
    }

    List<DepartamentoView> listarDepartamentos(Long empresaId) {
        return jdbc.query(
                "select departamento_id, empresa_id, nome from dama.departamento where empresa_id = ? order by nome",
                (rs, i) -> new DepartamentoView(rs.getLong("departamento_id"), rs.getLong("empresa_id"), rs.getString("nome")),
                empresaId);
    }

    java.util.Optional<DepartamentoView> buscarDepartamento(Long departamentoId) {
        return jdbc.query(
                "select departamento_id, empresa_id, nome from dama.departamento where departamento_id = ?",
                (rs, i) -> new DepartamentoView(rs.getLong("departamento_id"), rs.getLong("empresa_id"), rs.getString("nome")),
                departamentoId).stream().findFirst();
    }

    boolean departamentoPertenceAEmpresa(Long departamentoId, Long empresaId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from dama.departamento where departamento_id = ? and empresa_id = ?",
                Integer.class, departamentoId, empresaId);
        return n != null && n > 0;
    }

}
