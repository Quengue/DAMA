import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { grafico, serie } from '../graficos.js';
import { h, fmt, painel, stat, tabela, status, badge, progresso, botao, formulario, opcoes, toast, vazio } from '../ui.js';
import { seletorPessoa } from './comum.js';
import { navegar } from '../app.js';

export default async function (el, params, recarregar) {
    const gerencia = sessao.pode('AI_CREDITS_GERENCIAR');
    const { elemento: seletor, id } = await seletorPessoa(params, valor => navegar('ai-credits', { colaborador: valor }),
        'AI_CREDITS_GERENCIAR');
    const e = sessao.empresaId;
    const [saldo, niveis, historico, registros, atividades, regras] = await Promise.all([
        api.get(`/colaboradores/${id}/ai-credits/saldo`),
        api.get('/ai-credits/niveis'),
        api.get(`/colaboradores/${id}/ai-credits/historico`),
        api.get(`/colaboradores/${id}/atividades`),
        api.get(`/empresas/${e}/atividades-produtivas`),
        api.get(`/empresas/${e}/ai-credits/regras`)
    ]);
    const nomeAtividade = new Map(atividades.map(a => [a.atividadeId, a.nome]));
    const ordenados = [...niveis].sort((a, b) => Number(a.creditosMinimos) - Number(b.creditosMinimos));
    const proximo = ordenados.find(n => Number(n.creditosMinimos) > Number(saldo.saldoAtual));

    if (seletor) el.append(seletor);

    el.append(h('div', { class: 'grade cards' },
        stat('Saldo', fmt.num(saldo.saldoAtual), 'AI Credits disponíveis'),
        stat('Nível de autonomia', saldo.nivelAtual?.nome || '—', saldo.nivelAtual?.descricao),
        stat('Próximo nível', proximo?.nome || 'Nível máximo',
            proximo ? `Faltam ${fmt.num(Number(proximo.creditosMinimos) - Number(saldo.saldoAtual))} créditos` : null),
        stat('Atividades registradas', fmt.num(registros.length))));

    const base = Number(saldo.nivelAtual?.creditosMinimos || 0);
    el.append(painel('Níveis de autonomia de acesso à IA', [
        h('div', { class: 'niveis' }, ordenados.map(n => h('div', {
            class: 'nivel' + (Number(saldo.saldoAtual) >= Number(n.creditosMinimos) ? ' atingido' : '')
                + (saldo.nivelAtual?.nivelId === n.nivelId ? ' atual' : '')
        }, h('div', { class: 'n' }, n.nome), h('div', { class: 'm' }, `${fmt.num(n.creditosMinimos)} créditos`),
            n.descricao && h('div', { class: 'm' }, n.descricao)))),
        proximo && h('div', { style: 'margin-top:14px' },
            progresso(((Number(saldo.saldoAtual) - base) / (Number(proximo.creditosMinimos) - base)) * 100))
    ], { subtitulo: 'O nível é definido pelo saldo atual' }));

    // saldo acumulado ao longo do tempo
    const cronologico = [...historico].sort((a, b) => a.registradoEm.localeCompare(b.registradoEm));
    let acumulado = 0;
    const pontos = cronologico.map(t => {
        acumulado += (t.tipo === 'CREDITO' ? 1 : -1) * Number(t.quantidade);
        return acumulado;
    });
    const areaGrafico = h('div', { class: 'grafico baixo' });

    const acoes = gerencia ? [
        botao('Registrar atividade', () => registrarAtividade(id, atividades, recarregar)),
        botao('Crédito ou débito manual', () => lancarTransacao(id, recarregar), { tipo: 'primario' })
    ] : null;

    el.append(h('div', { class: 'grade duas' },
        painel('Saldo ao longo do tempo', cronologico.length ? areaGrafico : vazio('Sem movimentações.')),
        painel('Histórico', tabela([
            { titulo: 'Data', valor: t => fmt.dataHora(t.registradoEm) },
            { titulo: 'Tipo', valor: t => status(t.tipo) },
            { titulo: 'Motivo', valor: t => t.motivo },
            { titulo: 'Qtd.', classe: 'num', valor: t => (t.tipo === 'CREDITO' ? '+' : '−') + fmt.num(t.quantidade) }
        ], historico, { vazioTexto: 'Nenhuma movimentação.' }), { acoes })));

    if (cronologico.length) {
        grafico(areaGrafico, 'line', {
            labels: cronologico.map(t => fmt.data(t.registradoEm)),
            datasets: [serie('Saldo', pontos, 0, { fill: true, stepped: true })]
        }, { plugins: { legend: { display: false } } });
    }

    el.append(painel('Atividades registradas', tabela([
        { titulo: 'Data', valor: r => fmt.dataHora(r.registradoEm) },
        { titulo: 'Atividade', valor: r => nomeAtividade.get(r.atividadeId) || `#${r.atividadeId}` },
        { titulo: 'Qtd.', classe: 'num', valor: r => fmt.num(r.quantidade) }
    ], registros, { vazioTexto: 'Nenhuma atividade registrada.' })));

    el.append(catalogo(atividades, regras, gerencia, recarregar));
}

function catalogo(atividades, regras, gerencia, recarregar) {
    const e = sessao.empresaId;
    const regrasPorAtividade = new Map();
    for (const r of regras.filter(r => r.ativa)) {
        regrasPorAtividade.set(r.atividadeId, [...(regrasPorAtividade.get(r.atividadeId) || []), r]);
    }

    const novaAtividade = () => formulario('Nova atividade produtiva', [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true, inteiro: true },
        { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea' }
    ], async v => {
        await api.post(`/empresas/${e}/atividades-produtivas`, v);
        toast('Atividade cadastrada.');
        recarregar();
    });

    const novaRegra = () => formulario('Nova regra de crédito', [
        { nome: 'atividadeId', rotulo: 'Atividade', tipo: 'select', obrigatorio: true, opcoes: opcoes(atividades, a => a.atividadeId, a => a.nome) },
        { nome: 'creditosConcedidos', rotulo: 'Créditos por registro', tipo: 'number', obrigatorio: true },
        { nome: 'nome', rotulo: 'Nome da regra', obrigatorio: true },
        { nome: 'criterioElegibilidade', rotulo: 'Critério de elegibilidade', obrigatorio: true, ajuda: 'Ex.: relatório aprovado pelo gestor' }
    ], async v => {
        await api.post(`/empresas/${e}/ai-credits/regras`, { ...v, atividadeId: Number(v.atividadeId) });
        toast('Regra criada.');
        recarregar();
    });

    return painel('Atividades que geram AI Credits', tabela([
        { titulo: 'Atividade', valor: a => h('div', {}, h('div', { class: 'forte' }, a.nome), h('small', { class: 'muted' }, a.descricao || '')) },
        {
            titulo: 'Elegível', valor: a => regrasPorAtividade.has(a.atividadeId)
                ? badge('Gera créditos', 'sucesso') : badge('Não gera', '')
        },
        {
            titulo: 'Regra', valor: a => (regrasPorAtividade.get(a.atividadeId) || [])
                .map(r => `${r.nome}: +${fmt.num(r.creditosConcedidos)} (${r.criterioElegibilidade})`).join(' · ') || '—'
        }
    ], atividades, { vazioTexto: 'Nenhuma atividade cadastrada.' }), {
        subtitulo: 'Só atividades com regra ativa geram créditos',
        acoes: gerencia ? [botao('Nova atividade', novaAtividade), botao('Nova regra', novaRegra)] : null
    });
}

function registrarAtividade(colaboradorId, atividades, recarregar) {
    formulario('Registrar atividade realizada', [
        { nome: 'atividadeId', rotulo: 'Atividade', tipo: 'select', obrigatorio: true, inteiro: true, opcoes: opcoes(atividades.filter(a => a.ativa), a => a.atividadeId, a => a.nome) }
    ], async v => {
        const r = await api.post(`/colaboradores/${colaboradorId}/atividades`, { atividadeId: Number(v.atividadeId) });
        toast(r.elegivel ? `Atividade registrada: +${fmt.num(r.creditosConcedidos)} AI Credits.` : 'Atividade registrada (não gera créditos).');
        recarregar();
    }, { textoBotao: 'Registrar' });
}

function lancarTransacao(colaboradorId, recarregar) {
    formulario('Crédito ou débito manual', [
        { nome: 'tipo', rotulo: 'Tipo', tipo: 'select', obrigatorio: true, opcoes: [{ valor: 'CREDITO', rotulo: 'Crédito' }, { valor: 'DEBITO', rotulo: 'Débito (uso de créditos)' }] },
        { nome: 'quantidade', rotulo: 'Quantidade', tipo: 'number', obrigatorio: true },
        { nome: 'motivo', rotulo: 'Motivo', obrigatorio: true, inteiro: true }
    ], async v => {
        await api.post(`/colaboradores/${colaboradorId}/ai-credits/transacoes`, v);
        toast('Movimentação registrada.');
        recarregar();
    }, { textoBotao: 'Lançar' });
}
