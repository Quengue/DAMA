package br.com.dama.intelligence.conquista.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.conquista.api.*;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.shared.error.ConflictException;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
class ConquistaService implements ConquistaFacade {

    private final ConquistaRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final AuditoriaFacade auditoria;

    ConquistaService(ConquistaRepository repo, ColaboradorFacade colaboradores, OrganizacaoFacade organizacao,
                     AuditoriaFacade auditoria) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.auditoria = auditoria;
    }

    @Override
    public List<ConquistaView> listar(Long empresaId) {
        exigirEmpresa(empresaId);
        return repo.listar(empresaId);
    }

    @Override
    @Transactional
    public ConquistaView criar(Long empresaId, CreateConquistaCommand comando) {
        exigirEmpresa(empresaId);
        CriterioConquista criterio = CriterioConquista.de(comando.criterio())
                .orElseThrow(() -> new ValidationException("Critério inválido: " + comando.criterio()));
        if (criterio.automatico() && (comando.valorMinimo() == null || comando.valorMinimo().signum() <= 0)) {
            throw new ValidationException("Informe valorMinimo maior que zero para o critério " + criterio.name() + ".");
        }
        if (!criterio.automatico() && comando.valorMinimo() != null) {
            throw new ValidationException("Conquistas MANUAL não têm valorMinimo.");
        }

        Long id;
        try {
            id = repo.inserir(empresaId, comando.nome(), comando.descricao(), criterio.name(), comando.valorMinimo());
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Já existe uma conquista com este nome nesta empresa.");
        }
        auditoria.registrar(empresaId, UsuarioAtual.id(), "conquista", id.toString(), "CRIACAO", null);

        if (criterio.automatico()) {
            registrarConcessoes(empresaId, repo.concederAutomaticas(empresaId, null, id));
        }
        return buscar(id);
    }

    @Override
    @Transactional
    public ConquistaView alterarAtiva(Long conquistaId, boolean ativa) {
        ConquistaView conquista = buscar(conquistaId);
        repo.atualizarAtiva(conquistaId, ativa);
        auditoria.registrar(conquista.empresaId(), UsuarioAtual.id(), "conquista", conquistaId.toString(),
                ativa ? "ATIVACAO" : "DESATIVACAO", null);
        if (ativa && CriterioConquista.de(conquista.criterio()).map(CriterioConquista::automatico).orElse(false)) {
            registrarConcessoes(conquista.empresaId(), repo.concederAutomaticas(conquista.empresaId(), null, conquistaId));
        }
        return buscar(conquistaId);
    }

    @Override
    @Transactional
    public ConquistaObtidaView conceder(Long colaboradorId, Long conquistaId) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        ConquistaView conquista = buscar(conquistaId);
        if (!conquista.empresaId().equals(colaborador.empresaId())) {
            throw new ValidationException("A conquista não pertence à empresa do colaborador.");
        }
        if (!CriterioConquista.MANUAL.name().equals(conquista.criterio())) {
            throw new ValidationException("Só conquistas MANUAL podem ser concedidas manualmente; as demais são automáticas.");
        }
        if (!conquista.ativa()) {
            throw new ValidationException("A conquista está inativa.");
        }
        if (!colaborador.ativo()) {
            throw new ValidationException("O colaborador está inativo.");
        }
        if (repo.possui(colaboradorId, conquistaId)) {
            throw new ConflictException("O colaborador já possui esta conquista.");
        }

        repo.inserirConcessaoManual(colaboradorId, conquistaId, UsuarioAtual.id());
        auditoria.registrar(colaborador.empresaId(), UsuarioAtual.id(), "colaborador_conquista",
                colaboradorId + ":" + conquistaId, "CONCESSAO_MANUAL", null);
        return repo.buscarObtida(colaboradorId, conquistaId).orElseThrow();
    }

    @Override
    public List<ConquistaObtidaView> conquistasDoColaborador(Long colaboradorId) {
        colaboradores.buscarPorId(colaboradorId); // 404 se não existir
        return repo.conquistasDoColaborador(colaboradorId);
    }

    @Override
    @Transactional
    public int reavaliarEmpresa(Long empresaId) {
        exigirEmpresa(empresaId);
        List<long[]> concedidas = repo.concederAutomaticas(empresaId, null, null);
        registrarConcessoes(empresaId, concedidas);
        return concedidas.size();
    }

    /** Chamado pelo ConquistaEventListener a cada DesempenhoAtualizadoEvent. */
    @Transactional
    void avaliarColaborador(Long colaboradorId) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        if (!colaborador.ativo()) {
            return;
        }
        registrarConcessoes(colaborador.empresaId(), repo.concederAutomaticas(colaborador.empresaId(), colaboradorId, null));
    }

    @Override
    public EvolucaoView evolucao(Long colaboradorId) {
        ColaboradorView colaborador = colaboradores.buscarPorId(colaboradorId);
        BigDecimal pontos = repo.pontosTotais(colaboradorId);
        Posicao posicao = posicionar(pontos, repo.listarNiveis());
        return new EvolucaoView(colaboradorId, colaborador.nome(), pontos, posicao.atual(), posicao.proximo(),
                posicao.faltam(), posicao.percentual(), repo.conquistasDoColaborador(colaboradorId));
    }

    @Override
    public List<NivelEvolucaoView> listarNiveis() {
        return repo.listarNiveis();
    }

    @Override
    @Transactional
    public NivelEvolucaoView criarNivel(CreateNivelEvolucaoCommand comando) {
        try {
            return repo.inserirNivel(comando.nome(), comando.pontosMinimos(), comando.ordem(), comando.descricao());
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Já existe um nível com este nome ou esta ordem.");
        }
    }

    record Posicao(NivelEvolucaoView atual, NivelEvolucaoView proximo, BigDecimal faltam, int percentual) {
    }

    /**
     * Nível atual = maior nível cujo mínimo já foi atingido. percentual = progresso entre o mínimo do nível atual
     * e o do próximo (100 quando não há próximo nível).
     */
    static Posicao posicionar(BigDecimal pontos, List<NivelEvolucaoView> niveisOrdenados) {
        NivelEvolucaoView atual = null;
        NivelEvolucaoView proximo = null;
        for (NivelEvolucaoView nivel : niveisOrdenados) {
            if (pontos.compareTo(nivel.pontosMinimos()) >= 0) {
                atual = nivel;
            } else {
                proximo = nivel;
                break;
            }
        }
        if (proximo == null) {
            return new Posicao(atual, null, BigDecimal.ZERO, 100);
        }
        BigDecimal base = atual == null ? BigDecimal.ZERO : atual.pontosMinimos();
        BigDecimal faixa = proximo.pontosMinimos().subtract(base);
        int percentual = faixa.signum() == 0 ? 100 : pontos.subtract(base)
                .multiply(BigDecimal.valueOf(100))
                .divide(faixa, 0, RoundingMode.DOWN)
                .intValue();
        return new Posicao(atual, proximo, proximo.pontosMinimos().subtract(pontos), percentual);
    }

    private void registrarConcessoes(Long empresaId, List<long[]> concedidas) {
        for (long[] par : concedidas) {
            auditoria.registrar(empresaId, UsuarioAtual.id(), "colaborador_conquista", par[0] + ":" + par[1],
                    "CONCESSAO_AUTOMATICA", null);
        }
    }

    private ConquistaView buscar(Long conquistaId) {
        return repo.buscar(conquistaId).orElseThrow(() -> new NotFoundException("Conquista inexistente: " + conquistaId));
    }

    private void exigirEmpresa(Long empresaId) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }
}
