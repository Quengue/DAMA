package br.com.dama.intelligence.pontuacao.api;

import java.math.BigDecimal;
import java.util.List;

/** Contrato público do módulo pontuacao (RF07, RF27, RN02). O ranking (RF08/RF28) está em analytics. */
public interface PontuacaoFacade {
    List<RegraPontuacaoView> listarRegras(Long empresaId);

    RegraPontuacaoView criarRegra(Long empresaId, CreateRegraPontuacaoCommand comando);

    RegraPontuacaoView alterarRegraAtiva(Long regraId, boolean ativa);

    PontuacaoView lancar(Long colaboradorId, LancarPontosCommand comando);

    PontuacaoResumoView resumo(Long colaboradorId);

    /**
     * Uso interno entre módulos (ex.: desafio concluído): credita pontos cujo critério vem do próprio
     * chamador, sem passar por uma regra_pontuacao. Não exposto via HTTP.
     */
    PontuacaoView creditarPontos(Long colaboradorId, BigDecimal pontos, String descricao);
}
