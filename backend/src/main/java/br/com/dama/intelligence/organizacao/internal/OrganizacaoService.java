package br.com.dama.intelligence.organizacao.internal;

import br.com.dama.intelligence.organizacao.api.*;
import br.com.dama.intelligence.shared.error.ConflictException;
import br.com.dama.intelligence.shared.error.NotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class OrganizacaoService implements OrganizacaoFacade {

    private final OrganizacaoRepository repo;

    OrganizacaoService(OrganizacaoRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<EmpresaView> listarEmpresas() {
        return repo.listarEmpresas();
    }

    @Override
    public EmpresaView criarEmpresa(CreateEmpresaCommand comando) {
        Long id = repo.inserirEmpresa(comando.nome(), comando.setor(), comando.porte());
        return new EmpresaView(id, comando.nome(), comando.setor(), comando.porte());
    }

    @Override
    public List<DepartamentoView> listarDepartamentos(Long empresaId) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        return repo.listarDepartamentos(empresaId);
    }

    @Override
    public DepartamentoView criarDepartamento(Long empresaId, CreateDepartamentoCommand comando) {
        if (!repo.existeEmpresa(empresaId)) {
            throw new NotFoundException("Empresa inexistente: " + empresaId);
        }
        try {
            Long id = repo.inserirDepartamento(empresaId, comando.nome());
            return new DepartamentoView(id, empresaId, comando.nome());
        } catch (DuplicateKeyException e) {
            throw new ConflictException("Já existe um departamento com este nome nesta empresa.");
        }
    }

    @Override
    public boolean existeEmpresa(Long empresaId) {
        return repo.existeEmpresa(empresaId);
    }

    @Override
    public boolean departamentoPertenceAEmpresa(Long departamentoId, Long empresaId) {
        return repo.departamentoPertenceAEmpresa(departamentoId, empresaId);
    }

    @Override
    public DepartamentoView buscarDepartamento(Long departamentoId) {
        return repo.buscarDepartamento(departamentoId)
                .orElseThrow(() -> new NotFoundException("Departamento inexistente: " + departamentoId));
    }

}
