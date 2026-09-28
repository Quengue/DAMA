package br.com.dama.intelligence.aicredit.internal;

import br.com.dama.intelligence.aicredit.api.*;
import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.event.DesempenhoAtualizadoEvent;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
class AiCreditService implements AiCreditFacade {

    private final AiCreditRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final AuditoriaFacade auditoria;
    private final ApplicationEventPublisher eventos;

    AiCreditService(AiCreditRepository repo, ColaboradorFacade colaboradores, OrganizacaoFacade organizacao,
                    AuditoriaFacade auditoria, ApplicationEventPublisher eventos) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    // ---- RF19 ---------------------------------------------------------------------------------

    @Override
    public List<AtividadeProdutivaView> listarAtividades(Long empresaId) {
        exigirEmpresa(empresaId);
        return repo.listarAtividades(empresaId);
    }

    @Override
    @Transactional
    public AtividadeProdutivaView criarAtividade(Long empresaId, CreateAtividadeCommand comando) {
        exigirEmpresa(empresaId);
        AtividadeProdutivaView criada = repo.inserirAtividade(empresaId, comando.nome(), comando.descricao());
        auditoria.registrar(empresaId, UsuarioAtual.id(), "atividade_produtiva", criada.atividadeId().toString(), "CRIACAO", null);
        return criada;
    }

    @Override
    public boolean atividadePertenceAEmpresa(Long atividadeId, Long empresaId) {
        return repo.buscarAtividade(atividadeId).map(a -> a.empresaId().equals(empresaId)).orElse(false);
    }

    // ---- RF20 + RF21 + RF22 -------------------------------------------------------------------

    /**
     * Registra a atividade e avalia a elegibilidade: a atividade é elegível quando existe ao menos
     * uma regra ativa para ela (RF21). Para cada regra aplicável é lançado um CREDITO (RF22) com o
     * valor fixo da regra, uma vez por registro — a quantidade da atividade não multiplica o crédito.
     * O critério textual da regra (criterio_elegibilidade) é documentação para gestores: não é
     * interpretado automaticamente.
     */
    @Override
    @Transactional
    public RegistroAtividadeResultadoView registrarAtividade(Long colaboradorId, RegistrarAtividadeCommand comando) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        exigirAtivo(colaborador);

        AtividadeProdutivaView atividade = repo.buscarAtividade(comando.atividadeId())
                .orElseThrow(() -> new NotFoundException("Atividade inexistente: " + comando.atividadeId()));
        if (!atividade.empresaId().equals(colaborador.empresaId())) {
            throw new ValidationException("A atividade informada não pertence à empresa do colaborador.");
        }
        if (!atividade.ativa()) {
            throw new ValidationException("A atividade informada está inativa.");
        }

        BigDecimal quantidade = comando.quantidade() != null ? comando.quantidade() : BigDecimal.ONE;
        AtividadeRegistroView registro = repo.inserirRegistro(atividade.atividadeId(), colaboradorId, quantidade);
        auditoria.registrar(colaborador.empresaId(), UsuarioAtual.id(), "atividade_registro",
                registro.registroId().toString(), "CRIACAO", null);

        List<RegraCreditoView> regras = repo.regrasAtivasDaAtividade(atividade.atividadeId());
        BigDecimal totalConcedido = BigDecimal.ZERO;
        for (RegraCreditoView regra : regras) {
            TransacaoCreditoView transacao = repo.inserirTransacao(colaboradorId, regra.regraId(), registro.registroId(),
                    "CREDITO", regra.creditosConcedidos(), "Atividade elegível: " + atividade.nome());
            auditoria.registrar(colaborador.empresaId(), UsuarioAtual.id(), "ai_credit_transacao",
                    transacao.transacaoId().toString(), "CREDITO_AUTOMATICO", null);
            totalConcedido = totalConcedido.add(regra.creditosConcedidos());
        }

        eventos.publishEvent(new DesempenhoAtualizadoEvent(colaboradorId));
        return new RegistroAtividadeResultadoView(registro, !regras.isEmpty(), totalConcedido);
    }

    @Override
    public List<AtividadeRegistroView> listarRegistros(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId); // 404 se não existir
        return repo.listarRegistros(colaboradorId);
    }

    // ---- RF21 ---------------------------------------------------------------------------------

    @Override
    public List<RegraCreditoView> listarRegras(Long empresaId) {
        exigirEmpresa(empresaId);
        return repo.listarRegras(empresaId);
    }

    @Override
    @Transactional
    public RegraCreditoView criarRegra(Long empresaId, CreateRegraCreditoCommand comando) {
        exigirEmpresa(empresaId);
        if (!atividadePertenceAEmpresa(comando.atividadeId(), empresaId)) {
            throw new ValidationException("A atividade informada não existe ou não pertence a esta empresa.");
        }
        RegraCreditoView criada = repo.inserirRegra(empresaId, comando.atividadeId(), comando.nome(),
                comando.criterioElegibilidade(), comando.creditosConcedidos());
        auditoria.registrar(empresaId, UsuarioAtual.id(), "ai_credit_regra", criada.regraId().toString(), "CRIACAO", null);
        return criada;
    }

    @Override
    public ElegibilidadeView avaliarElegibilidade(Long atividadeId) {
        repo.buscarAtividade(atividadeId)
                .orElseThrow(() -> new NotFoundException("Atividade inexistente: " + atividadeId));
        List<RegraCreditoView> regras = repo.regrasAtivasDaAtividade(atividadeId);
        return new ElegibilidadeView(atividadeId, !regras.isEmpty(), regras);
    }

    // ---- RF22 / RF24 --------------------------------------------------------------------------

    @Override
    @Transactional
    public TransacaoCreditoView lancarTransacao(Long colaboradorId, LancarTransacaoCommand comando) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);

        TransacaoCreditoView transacao;
        if ("DEBITO".equals(comando.tipo())) {
            repo.bloquearSaldo(colaboradorId);
            if (repo.saldo(colaboradorId).compareTo(comando.quantidade()) < 0) {
                throw new ValidationException("Saldo de AI Credits insuficiente para este débito.");
            }
            transacao = repo.inserirTransacao(colaboradorId, null, null, "DEBITO", comando.quantidade(), comando.motivo());
        } else {
            exigirAtivo(colaborador);
            transacao = repo.inserirTransacao(colaboradorId, null, null, "CREDITO", comando.quantidade(), comando.motivo());
        }

        auditoria.registrar(colaborador.empresaId(), UsuarioAtual.id(), "ai_credit_transacao",
                transacao.transacaoId().toString(), comando.tipo(), null);
        if ("CREDITO".equals(comando.tipo())) {
            eventos.publishEvent(new DesempenhoAtualizadoEvent(colaboradorId));
        }
        return transacao;
    }

    @Override
    public List<TransacaoCreditoView> historico(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId);
        return repo.historico(colaboradorId);
    }

    // ---- RF23 / RF26 --------------------------------------------------------------------------

    @Override
    public SaldoCreditosView saldo(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId);
        BigDecimal saldo = repo.saldo(colaboradorId);
        return new SaldoCreditosView(colaboradorId, saldo, repo.nivelParaSaldo(saldo).orElse(null));
    }

    // ---- RF25 / RF26 --------------------------------------------------------------------------

    @Override
    public List<NivelAutonomiaView> listarNiveis() {
        return repo.listarNiveis();
    }

    @Override
    @Transactional
    public NivelAutonomiaView criarNivel(CreateNivelCommand comando) {
        return repo.inserirNivel(comando.nome(), comando.creditosMinimos(), comando.ordem(), comando.descricao());
    }

    // ---- helpers ------------------------------------------------------------------------------

    private void exigirEmpresa(Long empresaId) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }

    private void exigirAtivo(ColaboradorView colaborador) {
        if (!colaborador.ativo()) {
            throw new ValidationException("O colaborador está inativo.");
        }
    }
}
