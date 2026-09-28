package br.com.dama.intelligence.meta.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.indicador.api.IndicadorFacade;
import br.com.dama.intelligence.meta.api.*;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.event.DesempenhoAtualizadoEvent;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
class MetaService implements MetaFacade {

    private static final Set<String> STATUS_VALIDOS = Set.of("Em andamento", "Concluida", "Atrasada", "Cancelada");
    private static final Set<String> STATUS_ENCERRADOS = Set.of("Concluida", "Cancelada");

    private final MetaRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final IndicadorFacade indicadores;
    private final AuditoriaFacade auditoria;
    private final ApplicationEventPublisher eventos;

    MetaService(MetaRepository repo, ColaboradorFacade colaboradores, OrganizacaoFacade organizacao,
                IndicadorFacade indicadores, AuditoriaFacade auditoria, ApplicationEventPublisher eventos) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.indicadores = indicadores;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Override
    public List<MetaView> listar(Long empresaId, Long colaboradorId, Long departamentoId, String status) {
        exigirEmpresa(empresaId);
        if (status != null && !STATUS_VALIDOS.contains(status)) {
            throw new ValidationException("Status inválido. Use: " + String.join(", ", STATUS_VALIDOS));
        }
        return repo.listar(empresaId, colaboradorId, departamentoId, status);
    }

    @Override
    public MetaView buscar(Long metaId) {
        return repo.buscar(metaId).orElseThrow(() -> new NotFoundException("Meta inexistente: " + metaId));
    }

    @Override
    @Transactional
    public MetaView criar(Long empresaId, CreateMetaCommand comando) {
        exigirEmpresa(empresaId);

        // RN05 — a meta precisa estar associada a um colaborador ou a uma equipe
        if (comando.colaboradorId() == null && comando.departamentoId() == null) {
            throw new ValidationException("Informe colaboradorId ou departamentoId: toda meta deve ter um responsável (RN05).");
        }
        if (comando.dataFim().isBefore(comando.dataInicio())) {
            throw new ValidationException("dataFim não pode ser anterior a dataInicio.");
        }
        if (comando.colaboradorId() != null) {
            ColaboradorView colaborador = colaboradores.buscarPorId(comando.colaboradorId()); // 404 se não existir
            if (!colaborador.empresaId().equals(empresaId)) {
                throw new ValidationException("O colaborador informado não pertence a esta empresa.");
            }
        }
        if (comando.departamentoId() != null
                && !organizacao.departamentoPertenceAEmpresa(comando.departamentoId(), empresaId)) {
            throw new ValidationException("O departamento informado não pertence a esta empresa.");
        }
        if (comando.indicadorId() != null && !indicadores.pertenceAEmpresa(comando.indicadorId(), empresaId)) {
            throw new ValidationException("O indicador informado não pertence a esta empresa.");
        }

        Long metaId = repo.inserir(empresaId, comando.indicadorId(), comando.colaboradorId(), comando.departamentoId(),
                comando.descricao(), comando.valorAlvo(), comando.dataInicio(), comando.dataFim());

        auditoria.registrar(empresaId, UsuarioAtual.id(), "meta", metaId.toString(), "CRIACAO", null);
        return buscar(metaId);
    }

    @Override
    @Transactional
    public MetaView alterarStatus(Long metaId, String status) {
        if (!STATUS_VALIDOS.contains(status)) {
            throw new ValidationException("Status inválido. Use: " + String.join(", ", STATUS_VALIDOS));
        }
        MetaView meta = buscar(metaId);
        repo.atualizarStatus(metaId, status);
        auditoria.registrar(meta.empresaId(), UsuarioAtual.id(), "meta", metaId.toString(), "STATUS_" + status.toUpperCase(), null);
        if ("Concluida".equals(status)) {
            notificarConclusao(meta);
        }
        return buscar(metaId);
    }

    /**
     * RF30. A meta passa automaticamente para "Concluida" quando o valor atual atinge o alvo:
     * valorAtual >= valorAlvo, ou valorAtual <= valorAlvo se o indicador da meta for do tipo
     * "menor é melhor" (ex.: retrabalho).
     */
    @Override
    @Transactional
    public MetaProgressoView registrarProgresso(Long metaId, RegistrarProgressoCommand comando) {
        MetaView meta = buscar(metaId);
        if (STATUS_ENCERRADOS.contains(meta.status())) {
            throw new ValidationException("A meta já está encerrada (" + meta.status() + ").");
        }

        MetaProgressoView progresso = repo.inserirProgresso(metaId, comando.valorAtual());

        if (atingiuAlvo(meta, comando.valorAtual())) {
            repo.atualizarStatus(metaId, "Concluida");
            auditoria.registrar(meta.empresaId(), UsuarioAtual.id(), "meta", metaId.toString(), "CONCLUSAO_AUTOMATICA", null);
            notificarConclusao(meta);
        }
        return progresso;
    }

    @Override
    public List<MetaProgressoView> historicoProgresso(Long metaId) {
        buscar(metaId); // 404 se a meta não existir
        return repo.historico(metaId);
    }

    boolean atingiuAlvo(MetaView meta, java.math.BigDecimal valorAtual) {
        boolean maiorMelhor = meta.indicadorId() == null || indicadores.maiorMelhor(meta.indicadorId());
        int comparacao = valorAtual.compareTo(meta.valorAlvo());
        return maiorMelhor ? comparacao >= 0 : comparacao <= 0;
    }

    private void notificarConclusao(MetaView meta) {
        if (meta.colaboradorId() != null) {
            eventos.publishEvent(new DesempenhoAtualizadoEvent(meta.colaboradorId()));
        }
    }

    private void exigirEmpresa(Long empresaId) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }
}
