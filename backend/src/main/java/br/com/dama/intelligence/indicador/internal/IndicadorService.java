package br.com.dama.intelligence.indicador.internal;

import br.com.dama.intelligence.indicador.api.*;
import br.com.dama.intelligence.shared.error.NotFoundException;
import br.com.dama.intelligence.shared.error.ValidationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
class IndicadorService implements IndicadorFacade {

    private final IndicadorRepository repo;

    IndicadorService(IndicadorRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<IndicadorView> listarPorEmpresa(Long empresaId) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        return repo.listarPorEmpresa(empresaId);
    }

    @Override
    public IndicadorView criar(Long empresaId, CreateIndicadorCommand comando) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        Long id = repo.inserirIndicador(empresaId, comando.nome(), comando.descricao(), comando.unidade());
        return new IndicadorView(id, empresaId, comando.nome(), comando.descricao(), comando.unidade(), true);
    }

    @Override
    public IndicadorValorView registrarValor(Long indicadorId, RegistrarValorCommand comando) {
        if (!repo.existeIndicador(indicadorId)) {
            throw new NotFoundException("Indicador inexistente: " + indicadorId);
        }
        boolean temColaborador = comando.colaboradorId() != null;
        boolean temDepartamento = comando.departamentoId() != null;
        if (temColaborador == temDepartamento) {
            // RN06 — o indicador deve estar relacionado a um dado de desempenho concreto:
            // exatamente um colaborador OU um departamento, nunca os dois nem nenhum.
            throw new ValidationException("Informe exatamente um entre colaboradorId e departamentoId.");
        }

        Long valorId = repo.inserirValor(indicadorId, comando.colaboradorId(), comando.departamentoId(),
                comando.periodoReferencia(), comando.valor(), comando.registradoPor());

        return new IndicadorValorView(valorId, indicadorId, comando.colaboradorId(), comando.departamentoId(),
                comando.periodoReferencia(), comando.valor());
    }

    @Override
    public List<IndicadorValorView> historico(Long indicadorId, Long colaboradorId, Long departamentoId,
                                              LocalDate inicio, LocalDate fim) {
        if (!repo.existeIndicador(indicadorId)) {
            throw new NotFoundException("Indicador inexistente: " + indicadorId);
        }
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

}
