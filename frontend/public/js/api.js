import { sessao } from './sessao.js';

export class ApiErro extends Error {
    constructor(status, mensagem) {
        super(mensagem);
        this.status = status;
    }
}

const MENSAGEM_PADRAO = {
    401: 'Sessão inválida. Entre novamente.',
    403: 'Seu perfil não tem permissão para esta operação.',
    404: 'Registro não encontrado.',
    502: 'Backend indisponível.'
};

async function requisitar(metodo, caminho, corpo) {
    const headers = { Accept: 'application/json' };
    if (sessao.id) {
        headers['X-Colaborador-Id'] = String(sessao.id);
    }
    const opcoes = { method: metodo, headers };
    if (corpo !== undefined) {
        headers['Content-Type'] = 'application/json';
        opcoes.body = JSON.stringify(corpo);
    }

    let resposta;
    try {
        resposta = await fetch('/api' + caminho, opcoes);
    } catch {
        throw new ApiErro(0, 'Sem conexão com o servidor.');
    }

    const texto = await resposta.text();
    let dados = null;
    if (texto) {
        try {
            dados = JSON.parse(texto);
        } catch {
            dados = texto;
        }
    }

    if (!resposta.ok) {
        let mensagem = (dados && dados.message) || MENSAGEM_PADRAO[resposta.status] || `Erro ${resposta.status}`;
        if (resposta.status === 403) {
            mensagem = MENSAGEM_PADRAO[403];
        }
        if (dados && dados.fields) {
            mensagem += ' ' + Object.entries(dados.fields).map(([campo, erro]) => `${campo}: ${erro}`).join('; ');
        }
        throw new ApiErro(resposta.status, mensagem);
    }
    return dados;
}

export const api = {
    get: caminho => requisitar('GET', caminho),
    post: (caminho, corpo) => requisitar('POST', caminho, corpo),
    put: (caminho, corpo) => requisitar('PUT', caminho, corpo),
    patch: (caminho, corpo) => requisitar('PATCH', caminho, corpo),
    del: caminho => requisitar('DELETE', caminho)
};
