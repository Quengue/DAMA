package br.com.dama.intelligence.indicador.internal;

import br.com.dama.intelligence.indicador.api.IndicadorValorView;
import br.com.dama.intelligence.indicador.api.IndicadorView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
class IndicadorRepository {

    private final JdbcTemplate jdbc;

    IndicadorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_INDICADOR =
            "select indicador_id, empresa_id, nome, descricao, unidade, maior_melhor, ativo from dama.indicador ";

    Long inserirIndicador(Long empresaId, String nome, String descricao, String unidade, boolean maiorMelhor) {
        return jdbc.queryForObject(
                "insert into dama.indicador (empresa_id, nome, descricao, unidade, maior_melhor) values (?, ?, ?, ?, ?) " +
                        "returning indicador_id",
                Long.class, empresaId, nome, descricao, unidade, maiorMelhor);
    }

    Optional<IndicadorView> buscar(Long indicadorId) {
        return jdbc.query(SELECT_INDICADOR + "where indicador_id = ?", IndicadorRepository::mapearIndicador, indicadorId)
                .stream().findFirst();
    }

    List<IndicadorView> listarPorEmpresa(Long empresaId) {
        return jdbc.query(SELECT_INDICADOR + "where empresa_id = ? order by nome", IndicadorRepository::mapearIndicador, empresaId);
    }

    private static IndicadorView mapearIndicador(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new IndicadorView(rs.getLong("indicador_id"), rs.getLong("empresa_id"), rs.getString("nome"),
                rs.getString("descricao"), rs.getString("unidade"), rs.getBoolean("maior_melhor"), rs.getBoolean("ativo"));
    }

    Long inserirValor(Long indicadorId, Long colaboradorId, Long departamentoId, LocalDate periodo,
                       BigDecimal valor, Long registradoPor) {
        return jdbc.queryForObject(
                "insert into dama.indicador_valor (indicador_id, colaborador_id, departamento_id, periodo_referencia, valor, registrado_por) " +
                        "values (?, ?, ?, ?, ?, ?) returning indicador_valor_id",
                Long.class, indicadorId, colaboradorId, departamentoId, periodo, valor, registradoPor);
    }

    List<IndicadorValorView> historico(Long indicadorId, Long colaboradorId, Long departamentoId,
                                       LocalDate inicio, LocalDate fim) {
        StringBuilder sql = new StringBuilder(
                "select indicador_valor_id, indicador_id, colaborador_id, departamento_id, periodo_referencia, valor " +
                        "from dama.indicador_valor where indicador_id = ?");
        List<Object> params = new java.util.ArrayList<>(List.of(indicadorId));
        if (colaboradorId != null) {
            sql.append(" and colaborador_id = ?");
            params.add(colaboradorId);
        }
        if (departamentoId != null) {
            sql.append(" and departamento_id = ?");
            params.add(departamentoId);
        }
        if (inicio != null) {
            sql.append(" and periodo_referencia >= ?");
            params.add(inicio);
        }
        if (fim != null) {
            sql.append(" and periodo_referencia <= ?");
            params.add(fim);
        }
        sql.append(" order by periodo_referencia");
        return jdbc.query(sql.toString(), IndicadorRepository::mapearValor, params.toArray());
    }

    List<IndicadorValorView> ultimosValoresDoColaborador(Long colaboradorId) {
        return jdbc.query(
                "select distinct on (indicador_id) indicador_valor_id, indicador_id, colaborador_id, departamento_id, " +
                        "periodo_referencia, valor " +
                        "from dama.indicador_valor where colaborador_id = ? " +
                        "order by indicador_id, periodo_referencia desc",
                IndicadorRepository::mapearValor, colaboradorId);
    }

    private static IndicadorValorView mapearValor(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new IndicadorValorView(
                rs.getLong("indicador_valor_id"),
                rs.getLong("indicador_id"),
                rs.getObject("colaborador_id", Long.class),
                rs.getObject("departamento_id", Long.class),
                rs.getDate("periodo_referencia").toLocalDate(),
                rs.getBigDecimal("valor")
        );
    }

    boolean indicadorPertenceAEmpresa(Long indicadorId, Long empresaId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from dama.indicador where indicador_id = ? and empresa_id = ?",
                Integer.class, indicadorId, empresaId);
        return n != null && n > 0;
    }

}
