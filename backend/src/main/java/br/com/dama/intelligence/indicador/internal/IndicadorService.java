package br.com.dama.intelligence.indicador.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.indicador.api.*;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.security.UsuarioAtual;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
class IndicadorService implements IndicadorFacade {

    private final IndicadorRepository repo;
    private final ColaboradorFacade colaboradores;
    private final OrganizacaoFacade organizacao;
    private final AuditoriaFacade auditoria;

    IndicadorService(IndicadorRepository repo, ColaboradorFacade colaboradores, OrganizacaoFacade organizacao,
                     AuditoriaFacade auditoria) {
        this.repo = repo;
        this.colaboradores = colaboradores;
        this.organizacao = organizacao;
        this.auditoria = auditoria;
    }

    @Override
    public List<IndicadorView> listarPorEmpresa(Long empresaId) {
        exigirEmpresa(empresaId);
        return repo.listarPorEmpresa(empresaId);
    }

    @Override
    @Transactional
    public IndicadorView criar(Long empresaId, CreateIndicadorCommand comando) {
        exigirEmpresa(empresaId);
        boolean maiorMelhor = comando.maiorMelhor() == null || comando.maiorMelhor();
        Long id = repo.inserirIndicador(empresaId, comando.nome(), comando.descricao(), comando.unidade(), maiorMelhor);
        auditoria.registrar(empresaId, UsuarioAtual.id(), "indicador", id.toString(), "CRIACAO", null);
        return buscar(id);
    }

    @Override
    @Transactional
    public IndicadorValorView registrarValor(Long indicadorId, RegistrarValorCommand comando) {
        IndicadorView indicador = buscar(indicadorId);
        boolean temColaborador = comando.colaboradorId() != null;
        boolean temDepartamento = comando.departamentoId() != null;
        if (temColaborador == temDepartamento) {
            // RN06 — o valor pertence a exatamente um colaborador OU um departamento, nunca os dois nem nenhum.
            throw new ValidationException("Informe exatamente um entre colaboradorId e departamentoId.");
        }
        if (temColaborador && !colaboradores.buscarPorId(comando.colaboradorId()).empresaId().equals(indicador.empresaId())) {
            throw new ValidationException("O colaborador informado não pertence à empresa do indicador.");
        }
        if (temDepartamento && !organizacao.departamentoPertenceAEmpresa(comando.departamentoId(), indicador.empresaId())) {
            throw new ValidationException("O departamento informado não pertence à empresa do indicador.");
        }

        Long registradoPor = comando.registradoPor() != null ? comando.registradoPor() : UsuarioAtual.id();
        Long valorId = repo.inserirValor(indicadorId, comando.colaboradorId(), comando.departamentoId(),
                comando.periodoReferencia(), comando.valor(), registradoPor);
        auditoria.registrar(indicador.empresaId(), UsuarioAtual.id(), "indicador_valor", valorId.toString(), "REGISTRO", null);

        return new IndicadorValorView(valorId, indicadorId, comando.colaboradorId(), comando.departamentoId(),
                comando.periodoReferencia(), comando.valor());
    }

    @Override
    public List<IndicadorValorView> historico(Long indicadorId, Long colaboradorId, Long departamentoId,
                                              LocalDate inicio, LocalDate fim) {
        buscar(indicadorId);
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new ValidationException("fim não pode ser anterior a inicio.");
        }
        return repo.historico(indicadorId, colaboradorId, departamentoId, inicio, fim);
    }

    @Override
    public List<IndicadorValorView> ultimosValoresDoColaborador(Long colaboradorId) {
        return repo.ultimosValoresDoColaborador(colaboradorId);
    }

    @Override
    public boolean pertenceAEmpresa(Long indicadorId, Long empresaId) {
        return repo.indicadorPertenceAEmpresa(indicadorId, empresaId);
    }

    @Override
    public boolean maiorMelhor(Long indicadorId) {
        return buscar(indicadorId).maiorMelhor();
    }

    private IndicadorView buscar(Long indicadorId) {
        return repo.buscar(indicadorId).orElseThrow(() -> new NotFoundException("Indicador inexistente: " + indicadorId));
    }

    private void exigirEmpresa(Long empresaId) {
        if (!organizacao.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
    }

}
