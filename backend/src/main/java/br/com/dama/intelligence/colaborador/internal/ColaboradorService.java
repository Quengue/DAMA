package br.com.dama.intelligence.colaborador.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.*;
import br.com.dama.intelligence.shared.error.ConflictException;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class ColaboradorService implements ColaboradorFacade {

    private final ColaboradorRepository repo;
    private final AuditoriaFacade auditoria;

    ColaboradorService(ColaboradorRepository repo, AuditoriaFacade auditoria) {
        this.repo = repo;
        this.auditoria = auditoria;
    }

    @Override
    public List<ColaboradorView> listarPorEmpresa(Long empresaId) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        return repo.listarPorEmpresa(empresaId);
    }

    @Override
    public ColaboradorView buscarPorId(Long colaboradorId) {
        return repo.buscarPorId(colaboradorId)
                .orElseThrow(() -> new NotFoundException("Colaborador inexistente: " + colaboradorId));
    }

    @Override
    @Transactional
    public ColaboradorView criar(Long empresaId, CreateColaboradorCommand comando) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        if (!repo.departamentoPertenceAEmpresa(comando.departamentoId(), empresaId)) {
            throw new ValidationException("O departamento informado não pertence a esta empresa.");
        }
        if (repo.emailJaCadastrado(empresaId, comando.email(), null)) {
            throw new ConflictException("Já existe um colaborador com este e-mail nesta empresa.");
        }

        Long colaboradorId = repo.inserir(empresaId, comando.departamentoId(), comando.nome(), comando.email(),
                comando.cargo(), comando.senioridade(), comando.dataAdmissao());

        auditoria.registrar(empresaId, UsuarioAtual.id(), "colaborador", colaboradorId.toString(), "CRIACAO", null);
        return buscarPorId(colaboradorId);
    }

    @Override
    @Transactional
    public ColaboradorView atualizar(Long colaboradorId, UpdateColaboradorCommand comando) {
        ColaboradorView atual = buscarPorId(colaboradorId);

        if (!repo.departamentoPertenceAEmpresa(comando.departamentoId(), atual.empresaId())) {
            throw new ValidationException("O departamento informado não pertence a esta empresa.");
        }
        if (repo.emailJaCadastrado(atual.empresaId(), comando.email(), colaboradorId)) {
            throw new ConflictException("Já existe outro colaborador com este e-mail nesta empresa.");
        }

        repo.atualizar(colaboradorId, comando.departamentoId(), comando.nome(), comando.email(),
                comando.cargo(), comando.senioridade());

        auditoria.registrar(atual.empresaId(), UsuarioAtual.id(), "colaborador", colaboradorId.toString(), "ATUALIZACAO", null);
        return buscarPorId(colaboradorId);
    }

    @Override
    @Transactional
    public ColaboradorView alterarStatus(Long colaboradorId, boolean ativo) {
        ColaboradorView atual = buscarPorId(colaboradorId);
        repo.alterarStatus(colaboradorId, ativo);
        auditoria.registrar(atual.empresaId(), UsuarioAtual.id(), "colaborador", colaboradorId.toString(),
                ativo ? "REATIVACAO" : "DESLIGAMENTO", null);
        return buscarPorId(colaboradorId);
    }

    @Override
    public boolean existe(Long colaboradorId) {
        return repo.existe(colaboradorId);
    }
}
