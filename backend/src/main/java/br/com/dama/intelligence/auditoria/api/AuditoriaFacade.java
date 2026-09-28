package br.com.dama.intelligence.auditoria.api;

import java.util.List;

/**
 * RF11 (histórico) / RF38 (registrar movimentações para auditoria).
 * Módulos como colaborador, meta, pontuacao e ai_credit chamam
 * registrar(...) a cada criação/edição relevante; nenhuma outra
 * tabela precisa duplicar essa lógica.
 */
public interface AuditoriaFacade {
    void registrar(Long empresaId, Long colaboradorId, String entidade, String entidadeId, String acao, String detalhesJson);

    List<AuditoriaEventoView> listar(Long empresaId, String entidade, String entidadeId);
}
