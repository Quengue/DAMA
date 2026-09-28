// Listas de apoio (pessoas, departamentos, indicadores) com cache por sessão.
// Quem não tem COLABORADOR_LER ainda enxerga nomes pelo ranking, que exige só GAMIFICACAO_LER.

import { api } from './api.js';
import { sessao } from './sessao.js';

let cache = {};

export function limparCache() {
    cache = {};
}

async function lembrar(chave, carregar) {
    if (!(chave in cache)) {
        cache[chave] = carregar().catch(erro => {
            delete cache[chave];
            throw erro;
        });
    }
    return cache[chave];
}

/** Colaboradores da empresa: [{ colaboradorId, nome, departamentoId, ativo, ... }] */
export function pessoas() {
    return lembrar('pessoas', async () => {
        const e = sessao.empresaId;
        if (sessao.pode('COLABORADOR_LER')) {
            return api.get(`/empresas/${e}/colaboradores`);
        }
        if (sessao.pode('GAMIFICACAO_LER', 'ANALYTICS_LER')) {
            const ranking = await api.get(`/empresas/${e}/analytics/ranking-colaboradores`);
            return ranking.map(r => ({ colaboradorId: r.colaboradorId, nome: r.nome, departamentoId: r.departamentoId, ativo: true }));
        }
        return [sessao.colaborador];
    });
}

export async function pessoasAtivas() {
    return (await pessoas()).filter(p => p.ativo);
}

export function departamentos() {
    return lembrar('departamentos', async () => {
        if (sessao.pode('COLABORADOR_LER')) {
            return api.get(`/empresas/${sessao.empresaId}/departamentos`);
        }
        const c = sessao.colaborador;
        return [{ departamentoId: c.departamentoId, empresaId: c.empresaId, nome: c.departamentoNome }];
    });
}

export function indicadores() {
    return lembrar('indicadores', () => api.get(`/empresas/${sessao.empresaId}/indicadores`));
}

/** Funções id -> nome para exibir registros que só trazem ids. */
export async function nomes() {
    const [listaPessoas, listaDepartamentos] = await Promise.all([
        pessoas().catch(() => []),
        departamentos().catch(() => [])
    ]);
    const mapaPessoas = new Map(listaPessoas.map(p => [p.colaboradorId, p.nome]));
    const mapaDepartamentos = new Map(listaDepartamentos.map(d => [d.departamentoId, d.nome]));
    return {
        pessoa: id => id == null ? '—' : (mapaPessoas.get(id) || `Colaborador #${id}`),
        departamento: id => id == null ? '—' : (mapaDepartamentos.get(id) || `Equipe #${id}`)
    };
}
