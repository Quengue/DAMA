package br.com.dama.intelligence.desafio.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.desafio.api.*;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.pontuacao.api.PontuacaoFacade;
import br.com.dama.intelligence.shared.error.ConflictException;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.event.DesempenhoAtualizadoEvent;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
class DesafioService implements DesafioFacade {

    private static final Set<String> STATUS_VALIDOS = Set.of("Planejado", "Em andamento", "Encerrado", "Cancelado");
    private static final Set<String> STATUS_ABERTOS_A_INSCRICAO = Set.of("Planejado", "Em andamento");

    private final DesafioRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final PontuacaoFacade pontuacao;
    private final AuditoriaFacade auditoria;
    private final ApplicationEventPublisher eventos;

    DesafioService(DesafioRepository repo, ColaboradorFacade colaboradores, OrganizacaoFacade organizacao,
                   PontuacaoFacade pontuacao, AuditoriaFacade auditoria, ApplicationEventPublisher eventos) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.pontuacao = pontuacao;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Override
    public List<DesafioView> listar(Long empresaId, String status) {
        exigirEmpresa(empresaId);
        if (status != null && !STATUS_VALIDOS.contains(status)) {
            throw new ValidationException("Status inválido. Use: " + String.join(", ", STATUS_VALIDOS));
        }
        return repo.listar(empresaId, status);
    }

    @Override
    public DesafioView buscar(Long desafioId) {
        return repo.buscar(desafioId).orElseThrow(() -> new NotFoundException("Desafio inexistente: " + desafioId));
    }

    @Override
    @Transactional
    public DesafioView criar(Long empresaId, CreateDesafioCommand comando) {
        exigirEmpresa(empresaId);
        if (comando.dataFim().isBefore(comando.dataInicio())) {
            throw new ValidationException("dataFim não pode ser anterior a dataInicio.");
        }
        Long id = repo.inserir(empresaId, comando.nome(), comando.descricao(), comando.dataInicio(),
                comando.dataFim(), comando.pontosRecompensa());
        auditoria.registrar(empresaId, UsuarioAtual.id(), "desafio", id.toString(), "CRIACAO", null);
        return buscar(id);
    }

    @Override
    @Transactional
    public DesafioView alterarStatus(Long desafioId, String status) {
        if (!STATUS_VALIDOS.contains(status)) {
            throw new ValidationException("Status inválido. Use: " + String.join(", ", STATUS_VALIDOS));
        }
        DesafioView desafio = buscar(desafioId);
        repo.atualizarStatus(desafioId, status);
        auditoria.registrar(desafio.empresaId(), UsuarioAtual.id(), "desafio", desafioId.toString(),
                "STATUS_" + status.toUpperCase().replace(' ', '_'), null);
        return buscar(desafioId);
    }

    @Override
    @Transactional
    public ParticipanteView inscrever(Long desafioId, Long colaboradorId) {
        DesafioView desafio = buscar(desafioId);
        if (!STATUS_ABERTOS_A_INSCRICAO.contains(desafio.status())) {
            throw new ValidationException("O desafio não está aberto a inscrições (status: " + desafio.status() + ").");
        }
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        if (!colaborador.empresaId().equals(desafio.empresaId())) {
            throw new ValidationException("O colaborador não pertence à empresa deste desafio.");
        }
        if (!colaborador.ativo()) {
            throw new ValidationException("O colaborador está inativo.");
        }
        if (repo.buscarParticipante(desafioId, colaboradorId).isPresent()) {
            throw new ConflictException("O colaborador já está inscrito neste desafio.");
        }

        ParticipanteView participante = repo.inserirParticipante(desafioId, colaboradorId);
        auditoria.registrar(desafio.empresaId(), UsuarioAtual.id(), "desafio_participante",
                desafioId + ":" + colaboradorId, "INSCRICAO", null);
        return participante;
    }

    @Override
    public List<ParticipanteView> listarParticipantes(Long desafioId) {
        buscar(desafioId); // 404 se não existir
        return repo.listarParticipantes(desafioId);
    }

    @Override
    @Transactional
    public ParticipanteView concluir(Long desafioId, Long colaboradorId) {
        DesafioView desafio = buscar(desafioId);
        if (!"Em andamento".equals(desafio.status())) {
            throw new ValidationException("Só é possível concluir participação em desafio 'Em andamento'.");
        }
        ParticipanteView participante = exigirInscrito(desafioId, colaboradorId);

        BigDecimal recompensa = desafio.pontosRecompensa();
        repo.atualizarParticipante(desafioId, colaboradorId, "Concluido", recompensa);
        if (recompensa.signum() > 0) {
            pontuacao.creditarPontos(colaboradorId, recompensa, "Desafio concluído: " + desafio.nome());
        }
        auditoria.registrar(desafio.empresaId(), UsuarioAtual.id(), "desafio_participante",
                desafioId + ":" + colaboradorId, "CONCLUSAO", null);
        eventos.publishEvent(new DesempenhoAtualizadoEvent(colaboradorId));
        return repo.buscarParticipante(desafioId, colaboradorId).orElse(participante);
    }

    @Override
    @Transactional
    public ParticipanteView desistir(Long desafioId, Long colaboradorId) {
        DesafioView desafio = buscar(desafioId);
        exigirInscrito(desafioId, colaboradorId);

        repo.atualizarParticipante(desafioId, colaboradorId, "Desistente", BigDecimal.ZERO);
        auditoria.registrar(desafio.empresaId(), UsuarioAtual.id(), "desafio_participante",
                desafioId + ":" + colaboradorId, "DESISTENCIA", null);
        return repo.buscarParticipante(desafioId, colaboradorId).orElseThrow();
    }

    private ParticipanteView exigirInscrito(Long desafioId, Long colaboradorId) {
        ParticipanteView participante = repo.buscarParticipante(desafioId, colaboradorId)
                .orElseThrow(() -> new NotFoundException("O colaborador não está inscrito neste desafio."));
        if (!"Inscrito".equals(participante.status())) {
            throw new ConflictException("A participação já foi finalizada (status: " + participante.status() + ").");
        }
        return participante;
    }

    private void exigirEmpresa(Long empresaId) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }
}
