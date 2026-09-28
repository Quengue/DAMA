package br.com.dama.intelligence.conquista.internal;

import br.com.dama.intelligence.conquista.api.ConquistaObtidaView;
import br.com.dama.intelligence.conquista.api.ConquistaView;
import br.com.dama.intelligence.conquista.api.NivelEvolucaoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
class ConquistaRepository {

    private static final String SELECT_CONQUISTA =
            "select q.conquista_id, q.empresa_id, q.nome, q.descricao, q.criterio, q.valor_minimo, q.ativa, " +
                    "(select count(*) from dama.colaborador_conquista cc where cc.conquista_id = q.conquista_id) as obtidas " +
                    "from dama.conquista q ";

    private static final String SELECT_OBTIDA =
            "select cc.colaborador_id, q.conquista_id, q.nome, q.descricao, q.criterio, cc.obtida_em, cc.concedida_por_id " +
                    "from dama.colaborador_conquista cc join dama.conquista q on q.conquista_id = cc.conquista_id ";

    /**
     * Concede, de uma vez, todas as conquistas automáticas ativas cujo critério o colaborador já cumpre.
     * O mesmo SQL serve para um colaborador, para uma conquista ou para a empresa inteira (filtros opcionais).
     * "on conflict do nothing" torna a avaliação idempotente: quem já tem a conquista não a recebe de novo.
     */
    private static final String CONCEDER_AUTOMATICAS =
            "insert into dama.colaborador_conquista (colaborador_id, conquista_id) " +
                    "select c.colaborador_id, q.conquista_id " +
                    "from dama.colaborador c " +
                    "join dama.conquista q on q.empresa_id = c.empresa_id and q.ativa and q.criterio <> 'MANUAL' " +
                    "where c.ativo and c.empresa_id = ? %s " +
                    "and (case q.criterio " +
                    "  when 'PONTOS_TOTAIS' then (select coalesce(sum(p.pontos), 0) from dama.pontuacao p " +
                    "       where p.colaborador_id = c.colaborador_id) " +
                    "  when 'METAS_CONCLUIDAS' then (select count(*) from dama.meta m " +
                    "       where m.colaborador_id = c.colaborador_id and m.status = 'Concluida') " +
                    "  when 'DESAFIOS_CONCLUIDOS' then (select count(*) from dama.desafio_participante dp " +
                    "       where dp.colaborador_id = c.colaborador_id and dp.status = 'Concluido') " +
                    "  when 'RECONHECIMENTOS_RECEBIDOS' then (select count(*) from dama.reconhecimento r " +
                    "       where r.colaborador_id = c.colaborador_id) " +
                    "  when 'ATIVIDADES_REGISTRADAS' then (select count(*) from dama.atividade_registro ar " +
                    "       where ar.colaborador_id = c.colaborador_id) " +
                    "  when 'AI_CREDITS_ACUMULADOS' then (select coalesce(sum(t.quantidade), 0) from dama.ai_credit_transacao t " +
                    "       where t.colaborador_id = c.colaborador_id and t.tipo = 'CREDITO') " +
                    "end) >= q.valor_minimo " +
                    "on conflict (colaborador_id, conquista_id) do nothing " +
                    "returning colaborador_id, conquista_id";

    private final JdbcTemplate jdbc;

    ConquistaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    List<ConquistaView> listar(Long empresaId) {
        return jdbc.query(SELECT_CONQUISTA + "where q.empresa_id = ? order by q.ativa desc, q.nome",
                ConquistaRepository::mapearConquista, empresaId);
    }

    Optional<ConquistaView> buscar(Long conquistaId) {
        return jdbc.query(SELECT_CONQUISTA + "where q.conquista_id = ?", ConquistaRepository::mapearConquista, conquistaId)
                .stream().findFirst();
    }

    Long inserir(Long empresaId, String nome, String descricao, String criterio, BigDecimal valorMinimo) {
        return jdbc.queryForObject(
                "insert into dama.conquista (empresa_id, nome, descricao, criterio, valor_minimo) " +
                        "values (?, ?, ?, ?, ?) returning conquista_id",
                Long.class, empresaId, nome, descricao, criterio, valorMinimo);
    }

    void atualizarAtiva(Long conquistaId, boolean ativa) {
        jdbc.update("update dama.conquista set ativa = ? where conquista_id = ?", ativa, conquistaId);
    }

    /** Retorna os pares (colaboradorId, conquistaId) concedidos nesta chamada. */
    List<long[]> concederAutomaticas(Long empresaId, Long colaboradorId, Long conquistaId) {
        StringBuilder filtros = new StringBuilder();
        List<Object> params = new ArrayList<>();
        params.add(empresaId);
        if (colaboradorId != null) {
            filtros.append("and c.colaborador_id = ? ");
            params.add(colaboradorId);
        }
        if (conquistaId != null) {
            filtros.append("and q.conquista_id = ? ");
            params.add(conquistaId);
        }
        return jdbc.query(String.format(CONCEDER_AUTOMATICAS, filtros),
                (rs, i) -> new long[]{rs.getLong("colaborador_id"), rs.getLong("conquista_id")},
                params.toArray());
    }

    boolean possui(Long colaboradorId, Long conquistaId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from dama.colaborador_conquista where colaborador_id = ? and conquista_id = ?",
                Integer.class, colaboradorId, conquistaId);
        return n != null && n > 0;
    }

    void inserirConcessaoManual(Long colaboradorId, Long conquistaId, Long concedidaPorId) {
        jdbc.update("insert into dama.colaborador_conquista (colaborador_id, conquista_id, concedida_por_id) values (?, ?, ?)",
                colaboradorId, conquistaId, concedidaPorId);
    }

    Optional<ConquistaObtidaView> buscarObtida(Long colaboradorId, Long conquistaId) {
        return jdbc.query(SELECT_OBTIDA + "where cc.colaborador_id = ? and cc.conquista_id = ?",
                ConquistaRepository::mapearObtida, colaboradorId, conquistaId).stream().findFirst();
    }

    List<ConquistaObtidaView> conquistasDoColaborador(Long colaboradorId) {
        return jdbc.query(SELECT_OBTIDA + "where cc.colaborador_id = ? order by cc.obtida_em desc, q.nome",
                ConquistaRepository::mapearObtida, colaboradorId);
    }

    BigDecimal pontosTotais(Long colaboradorId) {
        BigDecimal soma = jdbc.queryForObject(
                "select coalesce(sum(pontos), 0) from dama.pontuacao where colaborador_id = ?", BigDecimal.class, colaboradorId);
        return soma == null ? BigDecimal.ZERO : soma;
    }

    List<NivelEvolucaoView> listarNiveis() {
        return jdbc.query(
                "select nivel_id, nome, pontos_minimos, ordem, descricao from dama.nivel_evolucao order by pontos_minimos, ordem",
                (rs, i) -> new NivelEvolucaoView(rs.getLong("nivel_id"), rs.getString("nome"),
                        rs.getBigDecimal("pontos_minimos"), rs.getInt("ordem"), rs.getString("descricao")));
    }

    NivelEvolucaoView inserirNivel(String nome, BigDecimal pontosMinimos, int ordem, String descricao) {
        return jdbc.queryForObject(
                "insert into dama.nivel_evolucao (nome, pontos_minimos, ordem, descricao) values (?, ?, ?, ?) " +
                        "returning nivel_id, nome, pontos_minimos, ordem, descricao",
                (rs, i) -> new NivelEvolucaoView(rs.getLong("nivel_id"), rs.getString("nome"),
                        rs.getBigDecimal("pontos_minimos"), rs.getInt("ordem"), rs.getString("descricao")),
                nome, pontosMinimos, ordem, descricao);
    }

    private static ConquistaView mapearConquista(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ConquistaView(
                rs.getLong("conquista_id"),
                rs.getLong("empresa_id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getString("criterio"),
                rs.getBigDecimal("valor_minimo"),
                rs.getBoolean("ativa"),
                rs.getLong("obtidas"));
    }

    private static ConquistaObtidaView mapearObtida(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ConquistaObtidaView(
                rs.getLong("colaborador_id"),
                rs.getLong("conquista_id"),
                rs.getString("nome"),
                rs.getString("descricao"),
                rs.getString("criterio"),
                rs.getTimestamp("obtida_em").toLocalDateTime(),
                rs.getObject("concedida_por_id", Long.class));
    }
}
