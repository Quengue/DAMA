package br.com.dama.intelligence.organizacao.api;

import java.util.List;

/**
 * Cadastro de empresa (tenant) e departamento. Não corresponde a nenhum RF do
 * documento — presume-se provisionado no onboarding comercial do DAMA — mas é
 * pré-requisito técnico para os demais módulos (colaborador, indicador etc.).
 */
public interface OrganizacaoFacade {
    List<EmpresaView> listarEmpresas();

    EmpresaView criarEmpresa(CreateEmpresaCommand comando);

    List<DepartamentoView> listarDepartamentos(Long empresaId);

    DepartamentoView criarDepartamento(Long empresaId, CreateDepartamentoCommand comando);

    boolean existeEmpresa(Long empresaId);

    boolean departamentoPertenceAEmpresa(Long departamentoId, Long empresaId);

}
