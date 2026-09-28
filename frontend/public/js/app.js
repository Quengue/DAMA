import { api } from './api.js';
import { sessao } from './sessao.js';
import { limparCache } from './dados.js';
import { destruirGraficos } from './graficos.js';
import { h, fmt, painel } from './ui.js';
import { USUARIOS_DEMO } from './config.js';

const ROTAS = [
    { grupo: 'Meu espaço', caminho: 'inicio', titulo: 'Meu painel', subtitulo: 'Indicadores, metas, pontos e AI Credits', icone: '◆', permissoes: ['DESEMPENHO_LER'], modulo: () => import('./paginas/inicio.js') },
    { grupo: 'Meu espaço', caminho: 'evolucao', titulo: 'Evolução e conquistas', subtitulo: 'Nível atual, próximo nível e conquistas', icone: '★', permissoes: ['GAMIFICACAO_LER'], modulo: () => import('./paginas/evolucao.js') },
    { grupo: 'Meu espaço', caminho: 'ai-credits', titulo: 'AI Credits', subtitulo: 'Saldo, nível de autonomia e histórico', icone: '⚡', permissoes: ['AI_CREDITS_LER'], modulo: () => import('./paginas/aicredits.js') },
    { grupo: 'Desempenho', caminho: 'ranking', titulo: 'Ranking e pontuação', subtitulo: 'Classificação da empresa e lançamento de pontos', icone: '▲', permissoes: ['GAMIFICACAO_LER', 'ANALYTICS_LER'], modulo: () => import('./paginas/ranking.js') },
    { grupo: 'Desempenho', caminho: 'metas', titulo: 'Metas', subtitulo: 'Metas individuais e de equipe', icone: '◎', permissoes: ['DESEMPENHO_LER'], modulo: () => import('./paginas/metas.js') },
    { grupo: 'Desempenho', caminho: 'indicadores', titulo: 'Indicadores', subtitulo: 'Evolução por período, colaborador e equipe', icone: '≋', permissoes: ['DESEMPENHO_LER'], modulo: () => import('./paginas/indicadores.js') },
    { grupo: 'Desempenho', caminho: 'equipes', titulo: 'Equipes', subtitulo: 'Dashboard por equipe e comparação entre equipes', icone: '▦', permissoes: ['DESEMPENHO_LER'], modulo: () => import('./paginas/equipes.js') },
    { grupo: 'Desempenho', caminho: 'desafios', titulo: 'Desafios', subtitulo: 'Desafios com recompensa em pontos', icone: '⚑', permissoes: ['GAMIFICACAO_LER'], modulo: () => import('./paginas/desafios.js') },
    { grupo: 'Desempenho', caminho: 'reconhecimentos', titulo: 'Reconhecimentos', subtitulo: 'Quem foi reconhecido e por quê', icone: '✦', permissoes: ['GAMIFICACAO_LER'], modulo: () => import('./paginas/reconhecimentos.js') },
    { grupo: 'Gestão', caminho: 'analises', titulo: 'Análises de gestão', subtitulo: 'Onde agir: metas, indicadores, equipes e engajamento', icone: '!', permissoes: ['ANALYTICS_LER'], modulo: () => import('./paginas/analises.js') },
    { grupo: 'Gestão', caminho: 'pessoas', titulo: 'Pessoas e perfis', subtitulo: 'Colaboradores, departamentos e permissões', icone: '☰', permissoes: ['COLABORADOR_LER'], modulo: () => import('./paginas/pessoas.js') },
    { grupo: 'Gestão', caminho: 'auditoria', titulo: 'Auditoria', subtitulo: 'Movimentações registradas na plataforma', icone: '⌕', permissoes: ['PERFIL_GERENCIAR'], modulo: () => import('./paginas/auditoria.js') }
];

const raiz = document.getElementById('app');
let conteudo;
let topo;
let menu;

function rotasPermitidas() {
    return ROTAS.filter(r => sessao.pode(...r.permissoes));
}

function lerHash() {
    const [caminho, consulta] = window.location.hash.replace(/^#\/?/, '').split('?');
    return { caminho, params: new URLSearchParams(consulta || '') };
}

export function navegar(caminho, params) {
    const consulta = params ? '?' + new URLSearchParams(params).toString() : '';
    window.location.hash = `#/${caminho}${consulta}`;
}

// ---------- login ----------

function telaLogin(mensagem = '') {
    destruirGraficos();
    const erro = h('div', { class: 'erro' }, mensagem);
    const campo = h('input', { type: 'number', min: '1', placeholder: 'Ex.: 4', id: 'login-id' });

    async function entrar(id) {
        erro.textContent = '';
        sessao.id = Number(id);
        try {
            const dados = await api.get('/me');
            sessao.definir(Number(id), dados);
            limparCache();
            montarLayout();
            const destino = rotasPermitidas()[0];
            if (!destino) {
                telaLogin('Este colaborador não tem nenhum perfil com permissões atribuídas.');
                sessao.limpar();
                return;
            }
            navegar(destino.caminho);
            renderizar();
        } catch (e) {
            sessao.limpar();
            erro.textContent = e.status === 404 ? 'Colaborador não encontrado.' : e.message;
        }
    }

    const form = h('form', {
        class: 'campo', onsubmit: e => {
            e.preventDefault();
            if (campo.value) entrar(campo.value);
        }
    },
        h('label', { for: 'login-id' }, 'ID do colaborador'),
        h('div', { class: 'linha' }, campo, h('button', { type: 'submit', class: 'btn primario' }, 'Entrar')),
        erro);

    raiz.replaceChildren(h('div', { class: 'login' },
        h('div', { class: 'caixa' },
            h('div', { class: 'logo' },
                h('h1', {}, 'DAMA ', h('span', {}, 'Intelligence')),
                h('small', {}, 'Desempenho, gamificação e AI Credits')),
            painel('Entrar', [
                form,
                h('p', { class: 'muted', style: 'margin-top:14px;font-size:12px' }, 'Usuários de demonstração:'),
                h('div', { class: 'demo' }, USUARIOS_DEMO.map(u =>
                    h('button', { type: 'button', class: 'btn', onclick: () => entrar(u.id) },
                        h('span', {}, `${u.nome} (#${u.id})`), h('small', {}, u.papel))))
            ], {
                subtitulo: 'A autenticação atual identifica o usuário pelo ID (cabeçalho X-Colaborador-Id).'
            }))));
    campo.focus();
}

// ---------- layout ----------

function montarLayout() {
    const c = sessao.colaborador;
    const grupos = new Map();
    for (const rota of rotasPermitidas()) {
        if (!grupos.has(rota.grupo)) grupos.set(rota.grupo, []);
        grupos.get(rota.grupo).push(rota);
    }

    menu = h('nav', { class: 'sidebar' },
        h('div', { class: 'logo' }, h('h1', {}, 'DAMA ', h('span', {}, 'Intel')), h('small', {}, 'Performance & AI Credits')),
        [...grupos.entries()].map(([grupo, rotas]) => h('div', { class: 'menu-grupo' },
            h('span', {}, grupo),
            rotas.map(r => h('a', { class: 'menu-item', href: `#/${r.caminho}`, 'data-rota': r.caminho },
                h('span', { class: 'icone' }, r.icone), r.titulo)))));

    const titulo = h('h2', {});
    const subtitulo = h('p', {});
    topo = {
        el: h('header', { class: 'topo' },
            h('div', {}, titulo, subtitulo),
            h('div', { class: 'usuario' },
                h('div', { class: 'avatar' }, fmt.iniciais(c.nome)),
                h('div', {}, h('div', { class: 'nome' }, c.nome),
                    h('div', { class: 'perfil' }, `${sessao.perfis.join(', ') || 'Sem perfil'} · ${c.departamentoNome}`)),
                h('button', {
                    type: 'button', class: 'btn pequeno', onclick: () => {
                        sessao.limpar();
                        limparCache();
                        window.location.hash = '';
                        telaLogin();
                    }
                }, 'Sair'))),
        titulo,
        subtitulo
    };
    conteudo = h('main', { class: 'conteudo' });

    raiz.replaceChildren(h('div', { class: 'layout' }, menu, h('div', { class: 'principal' }, topo.el, conteudo)));
}

let renderizacaoAtual = 0;

async function renderizar() {
    if (!sessao.colaborador) return;
    const { caminho, params } = lerHash();
    const permitidas = rotasPermitidas();
    const rota = permitidas.find(r => r.caminho === caminho) || permitidas[0];
    if (!rota) return;
    if (rota.caminho !== caminho) {
        navegar(rota.caminho);
        return;
    }

    const numero = ++renderizacaoAtual;
    destruirGraficos();
    menu.querySelectorAll('.menu-item').forEach(a => a.classList.toggle('ativo', a.dataset.rota === rota.caminho));
    topo.titulo.textContent = rota.titulo;
    topo.subtitulo.textContent = rota.subtitulo;
    document.title = `${rota.titulo} · DAMA Intelligence`;
    // a página desenha direto no DOM (gráficos precisam do contêiner já anexado para medir o tamanho)
    const carregando = h('div', { class: 'muted' }, 'Carregando…');
    const destino = h('div', { style: 'display:contents' });
    conteudo.replaceChildren(carregando, destino);

    try {
        const modulo = await rota.modulo();
        await modulo.default(destino, params, () => renderizar());
        carregando.remove();
    } catch (erro) {
        if (numero !== renderizacaoAtual) return;
        if (erro.status === 401) {
            sessao.limpar();
            telaLogin('Sessão inválida. Entre novamente.');
            return;
        }
        conteudo.replaceChildren(painel('Não foi possível carregar a página', h('p', { class: 'muted' }, erro.message)));
        console.error(erro);
    }
}

window.addEventListener('hashchange', renderizar);

async function iniciar() {
    if (!sessao.id) {
        telaLogin();
        return;
    }
    try {
        sessao.definir(sessao.id, await api.get('/me'));
        montarLayout();
        await renderizar();
    } catch {
        sessao.limpar();
        telaLogin();
    }
}

iniciar();
