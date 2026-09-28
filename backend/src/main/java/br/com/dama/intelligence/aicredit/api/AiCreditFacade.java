package br.com.dama.intelligence.aicredit.api;

import java.util.List;

/** Contrato público do módulo aicredit (RF12, RF19–RF26, RN04). */
public interface AiCreditFacade {
    // RF19 — atividades produtivas
    List<AtividadeProdutivaView> listarAtividades(Long empresaId);

    AtividadeProdutivaView criarAtividade(Long empresaId, CreateAtividadeCommand comando);

    boolean atividadePertenceAEmpresa(Long atividadeId, Long empresaId);

    // RF20 — atividades realizadas (+ RF21/RF22 automáticos)
    RegistroAtividadeResultadoView registrarAtividade(Long colaboradorId, RegistrarAtividadeCommand comando);

    List<AtividadeRegistroView> listarRegistros(Long colaboradorId);

    // RF21 — regras de elegibilidade
    List<RegraCreditoView> listarRegras(Long empresaId);

    RegraCreditoView criarRegra(Long empresaId, CreateRegraCreditoCommand comando);

    ElegibilidadeView avaliarElegibilidade(Long atividadeId);

    // RF22/RF24 — transações
    TransacaoCreditoView lancarTransacao(Long colaboradorId, LancarTransacaoCommand comando);

    List<TransacaoCreditoView> historico(Long colaboradorId);

    // RF23/RF26 — saldo e nível
    SaldoCreditosView saldo(Long colaboradorId);

    // RF25/RF26 — níveis de autonomia
    List<NivelAutonomiaView> listarNiveis();

    NivelAutonomiaView criarNivel(CreateNivelCommand comando);
}
