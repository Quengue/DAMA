// Usuário autenticado e suas permissões. O id vai no cabeçalho X-Colaborador-Id de cada chamada (ver api.js).

const CHAVE = 'dama.colaboradorId';

function lerSalvo() {
    try {
        const valor = window.localStorage.getItem(CHAVE);
        return valor ? Number(valor) : null;
    } catch {
        return null;
    }
}

export const sessao = {
    id: lerSalvo(),
    colaborador: null,
    perfis: [],
    permissoes: new Set(),

    get empresaId() {
        return this.colaborador?.empresaId;
    },

    pode(...permissoes) {
        return permissoes.some(p => this.permissoes.has(p));
    },

    definir(id, dados) {
        this.id = id;
        this.colaborador = dados.colaborador;
        this.perfis = dados.perfis;
        this.permissoes = new Set(dados.permissoes);
        try {
            window.localStorage.setItem(CHAVE, String(id));
        } catch {
            // sem storage (aba anônima etc.): a sessão vale só até recarregar a página
        }
    },

    limpar() {
        this.id = null;
        this.colaborador = null;
        this.perfis = [];
        this.permissoes = new Set();
        try {
            window.localStorage.removeItem(CHAVE);
        } catch {
            // ignorado
        }
    }
};
