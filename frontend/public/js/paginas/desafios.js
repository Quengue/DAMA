import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { nomes, pessoasAtivas } from '../dados.js';
import { h, fmt, painel, tabela, status, botao, formulario, opcoes, select, toast, modal, vazio, hojeIso } from '../ui.js';
import { navegar } from '../app.js';

const STATUS = ['Planejado', 'Em andamento', 'Encerrado', 'Cancelado'];

export default async function (el, params, recarregar) {
    const escreve = sessao.pode('GAMIFICACAO_ESCREVER');
    const filtro = params.get('status') || '';
    const desafios = await api.get(`/empresas/${sessao.empresaId}/desafios${filtro ? `?status=${encodeURIComponent(filtro)}` : ''}`);

    const acoes = [
        select([{ valor: '', rotulo: 'Todos os status' }, ...STATUS.map(s => ({ valor: s, rotulo: s }))],
            { valor: filtro, aoMudar: v => navegar('desafios', v ? { status: v } : undefined) }),
        escreve && botao('Novo desafio', () => novoDesafio(recarregar), { tipo: 'primario' })
    ];

    el.append(painel('Desafios', desafios.length === 0 ? vazio('Nenhum desafio encontrado.') :
        h('div', { class: 'grade tres' }, desafios.map(d => h('div', { class: 'painel', style: 'background:var(--panel-2)' },
            h('div', { class: 'linha', style: 'justify-content:space-between' }, h('div', { class: 'forte' }, d.nome), status(d.status)),
            h('p', { class: 'muted', style: 'margin:8px 0' }, d.descricao || ''),
            h('div', { class: 'linha muted', style: 'font-size:12px' }, `${fmt.data(d.dataInicio)} – ${fmt.data(d.dataFim)}`),
            h('div', { class: 'linha', style: 'margin:10px 0' },
                h('span', { class: 'badge acento' }, `+${fmt.num(d.pontosRecompensa)} pontos`),
                h('span', { class: 'badge' }, `${d.totalParticipantes} participante(s)`)),
            h('div', { class: 'linha' },
                botao('Participantes', () => participantes(d, escreve, recarregar), { pequeno: true }),
                escreve && botao('Status', () => alterarStatus(d, recarregar), { pequeno: true })))))
        , { acoes }));
}

function novoDesafio(recarregar) {
    formulario('Novo desafio', [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true, inteiro: true },
        { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea' },
        { nome: 'dataInicio', rotulo: 'Início', tipo: 'date', obrigatorio: true, valor: hojeIso() },
        { nome: 'dataFim', rotulo: 'Fim', tipo: 'date', obrigatorio: true, valor: hojeIso(30) },
        { nome: 'pontosRecompensa', rotulo: 'Pontos de recompensa', tipo: 'number', obrigatorio: true, valor: 30 }
    ], async v => {
        await api.post(`/empresas/${sessao.empresaId}/desafios`, v);
        toast('Desafio criado.');
        recarregar();
    });
}

function alterarStatus(desafio, recarregar) {
    formulario(`Status — ${desafio.nome}`, [
        { nome: 'status', rotulo: 'Status', tipo: 'select', obrigatorio: true, inteiro: true, valor: desafio.status, opcoes: STATUS.map(s => ({ valor: s, rotulo: s })) }
    ], async v => {
        await api.patch(`/desafios/${desafio.desafioId}/status`, v);
        toast('Status atualizado.');
        recarregar();
    });
}

async function participantes(desafio, escreve, recarregar) {
    const [lista, n, pessoas] = await Promise.all([
        api.get(`/desafios/${desafio.desafioId}/participantes`), nomes(), escreve ? pessoasAtivas() : Promise.resolve([])
    ]);
    const acao = async (caminho, mensagem, fechar) => {
        try {
            await api.post(`/desafios/${desafio.desafioId}/participantes/${caminho}`);
            toast(mensagem);
            fechar();
            recarregar();
        } catch (err) {
            toast(err.message, 'erro');
        }
    };

    const inscritos = new Set(lista.map(p => p.colaboradorId));
    const podeInscrever = escreve && ['Planejado', 'Em andamento'].includes(desafio.status);
    let fechar;
    const escolha = select(opcoes(pessoas.filter(p => !inscritos.has(p.colaboradorId)), p => p.colaboradorId, p => p.nome, { vazioRotulo: 'Escolha um colaborador' }), { classe: 'filtro' });

    fechar = modal(`Participantes — ${desafio.nome}`, [
        tabela([
            { titulo: 'Colaborador', valor: p => n.pessoa(p.colaboradorId) },
            { titulo: 'Situação', valor: p => status(p.status) },
            { titulo: 'Pontos', classe: 'num', valor: p => fmt.num(p.pontosObtidos) },
            escreve && {
                titulo: '', valor: p => p.status !== 'Inscrito' ? '' : h('div', { class: 'linha' },
                    desafio.status === 'Em andamento' && botao('Concluir', () => acao(`${p.colaboradorId}/concluir`, 'Participação concluída; pontos creditados.', fechar), { pequeno: true, tipo: 'primario' }),
                    botao('Desistência', () => acao(`${p.colaboradorId}/desistir`, 'Desistência registrada.', fechar), { pequeno: true }))
            }
        ].filter(Boolean), lista, { vazioTexto: 'Ninguém inscrito ainda.' }),
        podeInscrever && h('div', { class: 'linha', style: 'margin-top:14px' }, escolha,
            botao('Inscrever', async () => {
                if (!escolha.value) return;
                try {
                    await api.post(`/desafios/${desafio.desafioId}/participantes`, { colaboradorId: Number(escolha.value) });
                    toast('Colaborador inscrito.');
                    fechar();
                    recarregar();
                } catch (err) {
                    toast(err.message, 'erro');
                }
            }, { tipo: 'primario' }))
    ], close => h('button', { type: 'button', class: 'btn', onclick: close }, 'Fechar'));
}
