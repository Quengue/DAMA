import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { indicadores, nomes } from '../dados.js';
import { grafico, serie } from '../graficos.js';
import { h, fmt, painel, tabela, status, progresso, botao, formulario, opcoes, select, toast, modal, hojeIso, stat } from '../ui.js';
import { percentualMeta, opcoesResponsavel, lerResponsavel } from './comum.js';
import { navegar } from '../app.js';

const STATUS = ['Em andamento', 'Concluida', 'Atrasada', 'Cancelada'];

export default async function (el, params, recarregar) {
    const e = sessao.empresaId;
    const escreve = sessao.pode('DESEMPENHO_ESCREVER');
    const filtroStatus = params.get('status') || '';
    const filtroResponsavel = params.get('responsavel') || '';
    const { colaboradorId, departamentoId } = lerResponsavel(filtroResponsavel);

    const consulta = new URLSearchParams();
    if (filtroStatus) consulta.set('status', filtroStatus);
    if (colaboradorId) consulta.set('colaboradorId', colaboradorId);
    if (departamentoId) consulta.set('departamentoId', departamentoId);

    const [metas, listaIndicadores, n, responsaveis] = await Promise.all([
        api.get(`/empresas/${e}/metas?${consulta}`),
        indicadores(),
        nomes(),
        opcoesResponsavel({ incluirVazio: 'Todos os responsáveis' })
    ]);
    const indicador = new Map(listaIndicadores.map(i => [i.indicadorId, i]));
    const hoje = hojeIso();
    const vencida = m => ['Em andamento', 'Atrasada'].includes(m.status) && m.dataFim < hoje;

    const filtrar = (chave, valor) => {
        const novo = Object.fromEntries(params);
        if (valor) novo[chave] = valor; else delete novo[chave];
        navegar('metas', novo);
    };

    el.append(h('div', { class: 'grade cards' },
        stat('Metas', fmt.num(metas.length)),
        stat('Em andamento', fmt.num(metas.filter(m => m.status === 'Em andamento').length)),
        stat('Concluídas', fmt.num(metas.filter(m => m.status === 'Concluida').length)),
        stat('Vencidas', fmt.num(metas.filter(vencida).length), 'Prazo encerrado sem conclusão')));

    el.append(painel('Metas', tabela([
        { titulo: 'Meta', valor: m => h('div', {}, h('div', { class: 'forte' }, m.descricao),
                m.indicadorId && h('small', { class: 'muted' }, indicador.get(m.indicadorId)?.nome)) },
        { titulo: 'Responsável', valor: m => m.colaboradorId ? n.pessoa(m.colaboradorId) : `Equipe ${n.departamento(m.departamentoId)}` },
        { titulo: 'Prazo', valor: m => h('span', { class: vencida(m) ? 'forte' : '', style: vencida(m) ? 'color:var(--danger)' : null }, `${fmt.data(m.dataInicio)} – ${fmt.data(m.dataFim)}`) },
        { titulo: 'Progresso', valor: m => h('div', { style: 'min-width:160px' },
                progresso(percentualMeta(m, indicador.get(m.indicadorId)?.maiorMelhor !== false), { sucesso: m.status === 'Concluida' }),
                h('small', { class: 'muted' }, `${fmt.num(m.progressoAtual)} de ${fmt.num(m.valorAlvo)}`)) },
        { titulo: 'Status', valor: m => status(m.status) },
        {
            titulo: '', valor: m => h('div', { class: 'linha' },
                botao('Histórico', () => historico(m, indicador.get(m.indicadorId)), { pequeno: true }),
                escreve && !['Concluida', 'Cancelada'].includes(m.status) && botao('Progresso', () => registrarProgresso(m, recarregar), { pequeno: true, tipo: 'primario' }),
                escreve && botao('Status', () => alterarStatus(m, recarregar), { pequeno: true }))
        }
    ], metas, { vazioTexto: 'Nenhuma meta encontrada.' }), {
        acoes: [
            select([{ valor: '', rotulo: 'Todos os status' }, ...STATUS.map(s => ({ valor: s, rotulo: s === 'Concluida' ? 'Concluída' : s }))],
                { valor: filtroStatus, aoMudar: v => filtrar('status', v) }),
            select(responsaveis, { valor: filtroResponsavel, aoMudar: v => filtrar('responsavel', v) }),
            escreve && botao('Nova meta', () => novaMeta(listaIndicadores, recarregar), { tipo: 'primario' })
        ]
    }));
}

async function novaMeta(listaIndicadores, recarregar) {
    formulario('Nova meta', [
        { nome: 'responsavel', rotulo: 'Responsável', tipo: 'select', obrigatorio: true, inteiro: true, opcoes: await opcoesResponsavel() },
        { nome: 'descricao', rotulo: 'Objetivo', obrigatorio: true, inteiro: true },
        { nome: 'indicadorId', rotulo: 'Indicador (opcional)', tipo: 'select', opcoes: opcoes(listaIndicadores, i => i.indicadorId, i => `${i.nome}${i.maiorMelhor ? '' : ' (menor é melhor)'}`, { vazioRotulo: 'Sem indicador' }) },
        { nome: 'valorAlvo', rotulo: 'Valor alvo', tipo: 'number', obrigatorio: true },
        { nome: 'dataInicio', rotulo: 'Início', tipo: 'date', obrigatorio: true, valor: hojeIso() },
        { nome: 'dataFim', rotulo: 'Fim', tipo: 'date', obrigatorio: true, valor: hojeIso(90) }
    ], async v => {
        const { responsavel, indicadorId, ...resto } = v;
        await api.post(`/empresas/${sessao.empresaId}/metas`, {
            ...resto, ...lerResponsavel(responsavel), indicadorId: indicadorId ? Number(indicadorId) : null
        });
        toast('Meta criada.');
        recarregar();
    });
}

function registrarProgresso(meta, recarregar) {
    formulario(`Registrar progresso — ${meta.descricao}`, [
        { nome: 'valorAtual', rotulo: `Valor atual (alvo: ${fmt.num(meta.valorAlvo)})`, tipo: 'number', obrigatorio: true, inteiro: true, valor: meta.progressoAtual }
    ], async v => {
        await api.post(`/metas/${meta.metaId}/progresso`, v);
        const atualizada = await api.get(`/metas/${meta.metaId}`);
        toast(atualizada.status === 'Concluida' ? 'Meta atingida e concluída!' : 'Progresso registrado.');
        recarregar();
    }, { textoBotao: 'Registrar' });
}

function alterarStatus(meta, recarregar) {
    formulario('Alterar status', [
        { nome: 'status', rotulo: 'Status', tipo: 'select', obrigatorio: true, inteiro: true, valor: meta.status, opcoes: STATUS.map(s => ({ valor: s, rotulo: s })) }
    ], async v => {
        await api.patch(`/metas/${meta.metaId}/status`, v);
        toast('Status atualizado.');
        recarregar();
    });
}

async function historico(meta, indicador) {
    const registros = await api.get(`/metas/${meta.metaId}/progresso`);
    const area = h('div', { class: 'grafico baixo' });
    modal(`Histórico — ${meta.descricao}`, [
        registros.length ? area : h('p', { class: 'muted' }, 'Nenhum progresso registrado.'),
        tabela([
            { titulo: 'Data', valor: r => fmt.dataHora(r.registradoEm) },
            { titulo: 'Valor', classe: 'num', valor: r => fmt.comUnidade(r.valorAtual, indicador?.unidade) }
        ], registros)
    ], fechar => h('button', { type: 'button', class: 'btn', onclick: fechar }, 'Fechar'));
    if (registros.length) {
        grafico(area, 'line', {
            labels: registros.map(r => fmt.data(r.registradoEm)),
            datasets: [
                serie('Progresso', registros.map(r => Number(r.valorAtual)), 0, { fill: true }),
                serie('Alvo', registros.map(() => Number(meta.valorAlvo)), 2, { borderDash: [6, 4], pointRadius: 0 })
            ]
        });
    }
}
