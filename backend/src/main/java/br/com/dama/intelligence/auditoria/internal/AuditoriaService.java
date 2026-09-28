package br.com.dama.intelligence.auditoria.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaEventoView;
import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class AuditoriaService implements AuditoriaFacade {

    private final AuditoriaRepository repo;

    AuditoriaService(AuditoriaRepository repo) {
        this.repo = repo;
    }

    @Override
    public void registrar(Long empresaId, Long colaboradorId, String entidade, String entidadeId, String acao, String detalhesJson) {
        repo.inserir(empresaId, colaboradorId, entidade, entidadeId, acao, detalhesJson == null ? "{}" : detalhesJson);
    }

    @Override
    public List<AuditoriaEventoView> listar(Long empresaId, String entidade, String entidadeId) {
        return repo.listar(empresaId, entidade, entidadeId);
    }
}
