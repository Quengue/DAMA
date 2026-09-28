package br.com.dama.intelligence.pontuacao.internal;

import br.com.dama.intelligence.aicredit.api.AiCreditFacade;
import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.pontuacao.api.*;
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
class PontuacaoService implements PontuacaoFacade {

    private final PontuacaoRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final AiCreditFacade aiCredits;
    private final AuditoriaFacade auditoria;
    private final ApplicationEventPublisher eventos;

    PontuacaoService(PontuacaoRepository repo, ColaboradorFacade colaboradores, OrganizacaoFacade organizacao,
                     AiCreditFacade aiCredits, AuditoriaFacade auditoria, ApplicationEventPublisher eventos) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.aiCredits = aiCredits;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Override
    public List<RegraPontuacaoView> listarRegras(Long empresaId) {
        exigirEmpresa(empresaId);
        return repo.listarRegras(empresaId);
    }

    @Override
    @Transactional
    public RegraPontuacaoView criarRegra(Long empresaId, CreateRegraPontuacaoCommand comando) {
        exigirEmpresa(empresaId);
        if (comando.atividadeId() != null && !aiCredits.atividadePertenceAEmpresa(comando.atividadeId(), empresaId)) {
            throw new ValidationException("A atividade informada não existe ou não pertence a esta empresa.");
        }
        RegraPontuacaoView criada = repo.inserirRegra(empresaId, comando.atividadeId(), comando.nome(), comando.pontos());
        auditoria.registrar(empresaId, UsuarioAtual.id(), "regra_pontuacao", criada.regraId().toString(), "CRIACAO", null);
        return criada;
    }

    @Override
    @Transactional
    public RegraPontuacaoView alterarRegraAtiva(Long regraId, boolean ativa) {
        RegraPontuacaoView regra = repo.buscarRegra(regraId)
                .orElseThrow(() -> new NotFoundException("Regra de pontuação inexistente: " + regraId));
        repo.atualizarRegraAtiva(regraId, ativa);
        auditoria.registrar(regra.empresaId(), UsuarioAtual.id(), "regra_pontuacao", regraId.toString(),
                ativa ? "ATIVACAO" : "DESATIVACAO", null);
        return repo.buscarRegra(regraId).orElseThrow();
    }

    @Override
    @Transactional
    public PontuacaoView lancar(Long colaboradorId, LancarPontosCommand comando) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        exigirAtivo(colaborador);

        RegraPontuacaoView regra = repo.buscarRegra(comando.regraId())
                .orElseThrow(() -> new NotFoundException("Regra de pontuação inexistente: " + comando.regraId()));
        if (!regra.empresaId().equals(colaborador.empresaId())) {
            throw new ValidationException("A regra informada não pertence à empresa do colaborador.");
        }
        if (!regra.ativa()) {
            throw new ValidationException("A regra informada está inativa.");
        }

        String descricao = comando.descricao() != null && !comando.descricao().isBlank() ? comando.descricao() : regra.nome();
        PontuacaoView lancada = repo.inserirPontuacao(colaboradorId, regra.regraId(), regra.pontos(), descricao);
        auditoria.registrar(colaborador.empresaId(), UsuarioAtual.id(), "pontuacao", lancada.pontuacaoId().toString(), "LANCAMENTO", null);
        eventos.publishEvent(new DesempenhoAtualizadoEvent(colaboradorId));
        return lancada;
    }

    @Override
    public PontuacaoResumoView resumo(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId); // 404 se não existir
        return new PontuacaoResumoView(colaboradorId, repo.total(colaboradorId), repo.historico(colaboradorId));
    }

    @Override
    @Transactional
    public PontuacaoView creditarPontos(Long colaboradorId, BigDecimal pontos, String descricao) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        PontuacaoView lancada = repo.inserirPontuacao(colaboradorId, null, pontos, descricao);
        auditoria.registrar(colaborador.empresaId(), UsuarioAtual.id(), "pontuacao", lancada.pontuacaoId().toString(),
                "LANCAMENTO_AUTOMATICO", null);
        eventos.publishEvent(new DesempenhoAtualizadoEvent(colaboradorId));
        return lancada;
    }

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
