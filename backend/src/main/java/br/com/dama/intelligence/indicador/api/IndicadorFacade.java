package br.com.dama.intelligence.indicador.api;

import java.time.LocalDate;
import java.util.List;

/** RF03 (registrar), RF06/RF35 (evolução) e base para RF05/RF13 (dashboard). */
public interface IndicadorFacade {
    List<IndicadorView> listarPorEmpresa(Long empresaId);

    IndicadorView criar(Long empresaId, CreateIndicadorCommand comando);

    IndicadorValorView registrarValor(Long indicadorId, RegistrarValorCommand comando);

    /** RF06/RF34/RF35: evolução do indicador, opcionalmente filtrada por colaborador, departamento e período [inicio, fim]. */
    List<IndicadorValorView> historico(Long indicadorId, Long colaboradorId, Long departamentoId, LocalDate inicio, LocalDate fim);

    /** Último valor de cada indicador ativo da empresa para um colaborador — usado pelo dashboard (RF05). */
    List<IndicadorValorView> ultimosValoresDoColaborador(Long colaboradorId);

    boolean pertenceAEmpresa(Long indicadorId, Long empresaId);

    /** false quando o indicador é do tipo "menor é melhor". */
    boolean maiorMelhor(Long indicadorId);

}
