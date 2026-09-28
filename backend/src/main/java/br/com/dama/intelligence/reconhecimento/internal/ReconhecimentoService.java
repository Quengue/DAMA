package br.com.dama.intelligence.reconhecimento.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.reconhecimento.api.CreateReconhecimentoCommand;
import br.com.dama.intelligence.reconhecimento.api.ReconhecimentoFacade;
import br.com.dama.intelligence.reconhecimento.api.ReconhecimentoView;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.event.DesempenhoAtualizadoEvent;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class ReconhecimentoService implements ReconhecimentoFacade {

    private final ReconhecimentoRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final AuditoriaFacade auditoria;
    private final ApplicationEventPublisher eventos;

    ReconhecimentoService(ReconhecimentoRepository repo, ColaboradorFacade colaboradores,
                          OrganizacaoFacade organizacao, AuditoriaFacade auditoria, ApplicationEventPublisher eventos) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Override
    public List<ReconhecimentoView> listarPorEmpresa(Long empresaId) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        return repo.listarPorEmpresa(empresaId);
    }

    @Override
    public List<ReconhecimentoView> listarDoColaborador(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId); // 404 se não existir
        return repo.listarDoColaborador(colaboradorId);
    }

    @Override
    @Transactional
    public ReconhecimentoView criar(Long empresaId, CreateReconhecimentoCommand comando) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        ColaboradorView recebedor = colaboradores.buscarPorId(comando.colaboradorId());
        if (!recebedor.empresaId().equals(empresaId)) {
            throw new ValidationException("O colaborador reconhecido não pertence a esta empresa.");
        }
        if (!recebedor.ativo()) {
            throw new ValidationException("O colaborador reconhecido está inativo.");
        }

        ReconhecimentoView criado = repo.inserir(empresaId, comando.colaboradorId(), UsuarioAtual.id(),
                comando.tipo(), comando.descricao());
        auditoria.registrar(empresaId, UsuarioAtual.id(), "reconhecimento", criado.reconhecimentoId().toString(), "CRIACAO", null);
        eventos.publishEvent(new DesempenhoAtualizadoEvent(comando.colaboradorId()));
        return criado;
    }
}
