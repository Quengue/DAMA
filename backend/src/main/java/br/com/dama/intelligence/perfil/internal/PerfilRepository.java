package br.com.dama.intelligence.perfil.internal;

import br.com.dama.intelligence.perfil.api.PerfilView;
import br.com.dama.intelligence.perfil.api.PermissaoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Acesso a dados de dama.perfil / dama.permissao / dama.perfil_permissao / dama.colaborador_perfil. */
@Repository
class PerfilRepository {

    private final JdbcTemplate jdbc;

    PerfilRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    boolean existePerfil(Long perfilId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from dama.perfil where perfil_id = ?", Integer.class, perfilId);
        return n != null && n > 0;
    }

    List<Long> resolverPermissoes(List<String> codigos) {
        if (codigos.isEmpty()) return List.of();
        String placeholders = String.join(",", codigos.stream().map(c -> "?").toList());
        return jdbc.queryForList(
                "select permissao_id from dama.permissao where codigo in (" + placeholders + ")",
                Long.class, codigos.toArray());
    }

    Long inserirPerfil(String nome, String descricao) {
        return jdbc.queryForObject(
                "insert into dama.perfil (nome, descricao) values (?, ?) returning perfil_id",
                Long.class, nome, descricao);
    }

    void vincularPermissoes(Long perfilId, List<Long> permissaoIds) {
        for (Long permissaoId : permissaoIds) {
            jdbc.update(
                    "insert into dama.perfil_permissao (perfil_id, permissao_id) values (?, ?) " +
                            "on conflict do nothing",
                    perfilId, permissaoId);
        }
    }

    List<PerfilView> listarPerfis() {
        List<Map.Entry<Long, String[]>> base = jdbc.query(
                "select perfil_id, nome, descricao from dama.perfil order by nome",
                (rs, i) -> Map.entry(rs.getLong("perfil_id"),
                        new String[]{rs.getString("nome"), rs.getString("descricao")}));

        return base.stream()
                .map(e -> new PerfilView(e.getKey(), e.getValue()[0], e.getValue()[1], permissoesDoPerfil(e.getKey())))
                .toList();
    }

    List<String> permissoesDoPerfil(Long perfilId) {
        return jdbc.queryForList(
                "select pm.codigo from dama.perfil_permissao pp " +
                        "join dama.permissao pm on pm.permissao_id = pp.permissao_id " +
                        "where pp.perfil_id = ? order by pm.codigo",
                String.class, perfilId);
    }

    List<PermissaoView> listarPermissoes() {
        return jdbc.query(
                "select permissao_id, codigo, descricao from dama.permissao order by codigo",
                (rs, i) -> new PermissaoView(rs.getLong("permissao_id"), rs.getString("codigo"), rs.getString("descricao")));
    }

    void atribuirPerfil(Long colaboradorId, Long perfilId) {
        jdbc.update(
                "insert into dama.colaborador_perfil (colaborador_id, perfil_id) values (?, ?) " +
                        "on conflict do nothing",
                colaboradorId, perfilId);
    }

    void removerPerfil(Long colaboradorId, Long perfilId) {
        jdbc.update(
                "delete from dama.colaborador_perfil where colaborador_id = ? and perfil_id = ?",
                colaboradorId, perfilId);
    }

    List<PerfilView> perfisDoColaborador(Long colaboradorId) {
        List<Long> perfilIds = jdbc.queryForList(
                "select perfil_id from dama.colaborador_perfil where colaborador_id = ?",
                Long.class, colaboradorId);
        return listarPerfis().stream().filter(p -> perfilIds.contains(p.perfilId())).toList();
    }

    Set<String> permissoesDoColaborador(Long colaboradorId) {
        return Set.copyOf(jdbc.queryForList(
                "select distinct pm.codigo from dama.colaborador_perfil cp " +
                        "join dama.perfil_permissao pp on pp.perfil_id = cp.perfil_id " +
                        "join dama.permissao pm on pm.permissao_id = pp.permissao_id " +
                        "where cp.colaborador_id = ?",
                String.class, colaboradorId));
    }
}
