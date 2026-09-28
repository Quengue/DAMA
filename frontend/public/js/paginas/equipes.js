import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { departamentos, indicadores } from '../dados.js';
import { grafico, CORES } from '../graficos.js';
import { h, fmt, painel, stat, tabela, select, vazio } from '../ui.js';
import { navegar } from '../app.js';

export default async function (el, params) {
    const listaEquipes = await departamentos();
    const departamentoId = Number(params.get('dep')) || sessao.colaborador.departamentoId;
    const equipe = await api.get(`/departamentos/${departamentoId}/dashboard`);

    el.append(painel(null, h('div', { class: 'linha' },
        h('span', { class: 'muted' }, 'Equipe'),
        select(listaEquipes.map(d => ({ valor: d.departamentoId, rotulo: d.nome })), {
            valor: departamentoId, aoMudar: v => navegar('equipes', { dep: v })
        }))));

    el.append(h('div', { class: 'grade cards' },
        stat('Colaboradores ativos', fmt.num(equipe.colaboradoresAtivos)),
        stat('Pontos da equipe', fmt.num(equipe.pontosTotais), equipe.posicaoNoRanking ? `${equipe.posicaoNoRanking}º no ranking de equipes` : null),
        stat('AI Credits', fmt.num(equipe.aiCreditsSaldo), 'Saldo somado dos membros'),
        stat('Conquistas', fmt.num(equipe.conquistasObtidas)),
        stat('Metas em andamento', fmt.num(equipe.metasEmAndamento), `${fmt.num(equipe.metasConcluidas)} concluídas`),
        stat('Metas vencidas', fmt.num(equipe.metasVencidas))));

    el.append(h('div', { class: 'grade duas' },
        painel(`Membros de ${equipe.nome}`, tabela([
            { titulo: 'Colaborador', valor: m => h('div', {}, h('div', { class: 'forte' }, m.nome), h('small', { class: 'muted' }, m.cargo || '')) },
            { titulo: 'Pontos', classe: 'num', valor: m => fmt.num(m.pontosTotais) },
            { titulo: 'AI Credits', classe: 'num', valor: m => fmt.num(m.aiCreditsSaldo) },
            { titulo: 'Conquistas', classe: 'num', valor: m => fmt.num(m.conquistas) }
        ], equipe.membros, { destacar: m => m.colaboradorId === sessao.id, vazioTexto: 'Nenhum membro ativo.' })),
        painel('Indicadores da equipe', equipe.indicadores.length === 0 ? vazio('Nenhum indicador registrado para a equipe.') :
            h('div', { class: 'lista' }, equipe.indicadores.map(i => h('div', { class: 'lista-item' },
                h('div', {}, h('div', { class: 'titulo' }, i.indicadorNome), h('div', { class: 'sub' }, `Referência ${fmt.mes(i.periodoReferencia)}`)),
                h('div', { class: 'forte' }, fmt.comUnidade(i.ultimoValor, i.unidade))))),
            { subtitulo: 'Valor mais recente de cada indicador' })));

    if (sessao.pode('ANALYTICS_LER')) {
        await comparacao(el, params);
    }
}

async function comparacao(el, params) {
    const e = sessao.empresaId;
    const [ranking, todos, listaCompleta] = await Promise.all([
        api.get(`/empresas/${e}/analytics/ranking-departamentos`),
        api.get(`/empresas/${e}/analytics/indicadores-departamentos`),
        indicadores()
    ]);
    // só faz sentido comparar indicadores que têm valores registrados por equipe
    const comValores = new Set(todos.map(v => v.indicadorId));
    const lista = listaCompleta.filter(i => comValores.has(i.indicadorId));
    const indicadorId = Number(params.get('ind')) || lista[0]?.indicadorId;
    const porEquipe = todos.filter(v => v.indicadorId === indicadorId);
    const indicador = lista.find(i => i.indicadorId === indicadorId);

    const areaPontos = h('div', { class: 'grafico baixo' });
    const areaIndicador = h('div', { class: 'grafico baixo' });

    el.append(painel('Comparação entre equipes', tabela([
        { titulo: '#', valor: r => `${r.posicao}º` },
        { titulo: 'Equipe', valor: r => h('a', { href: `#/equipes?dep=${r.departamentoId}` }, r.departamentoNome) },
        { titulo: 'Ativos', classe: 'num', valor: r => fmt.num(r.colaboradoresAtivos) },
        { titulo: 'Pontos', classe: 'num', valor: r => fmt.num(r.pontosTotais) },
        { titulo: 'Pontos por pessoa', classe: 'num', valor: r => fmt.num(r.colaboradoresAtivos ? Number(r.pontosTotais) / r.colaboradoresAtivos : 0) }
    ], ranking), { subtitulo: 'Sempre dentro da mesma empresa' }));

    el.append(h('div', { class: 'grade duas' },
        painel('Pontos por pessoa', areaPontos),
        painel(indicador ? `${indicador.nome} por equipe` : 'Indicador por equipe', porEquipe.length ? areaIndicador : vazio('Nenhuma equipe tem valores deste indicador.'), {
            acoes: lista.length ? select(lista.map(i => ({ valor: i.indicadorId, rotulo: i.nome })), {
                valor: indicadorId, aoMudar: v => navegar('equipes', { ...Object.fromEntries(params), ind: v })
            }) : null,
            subtitulo: porEquipe.length ? `Valor mais recente · ${indicador?.maiorMelhor === false ? 'menor é melhor' : 'maior é melhor'}` : null
        })));

    grafico(areaPontos, 'bar', {
        labels: ranking.map(r => r.departamentoNome),
        datasets: [{
            label: 'Pontos por pessoa', borderRadius: 6, backgroundColor: ranking.map((_, i) => CORES[i % CORES.length] + 'cc'),
            data: ranking.map(r => r.colaboradoresAtivos ? Number(r.pontosTotais) / r.colaboradoresAtivos : 0)
        }]
    }, { plugins: { legend: { display: false } } });

    if (porEquipe.length) {
        grafico(areaIndicador, 'bar', {
            labels: porEquipe.map(v => v.departamentoNome),
            datasets: [{ label: indicador?.unidade || 'Valor', data: porEquipe.map(v => Number(v.valor)), backgroundColor: '#5aa9ffcc', borderRadius: 6 }]
        }, { plugins: { legend: { display: false } } });
    }
}
