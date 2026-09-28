import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { indicadores } from '../dados.js';
import { grafico, serie } from '../graficos.js';
import { h, fmt, painel, stat, tabela, status, progresso, select, vazio } from '../ui.js';
import { percentualMeta, seletorPessoa } from './comum.js';
import { navegar } from '../app.js';

export default async function (el, params) {
    const { elemento: seletor, id } = await seletorPessoa(params, valor => navegar('inicio', { colaborador: valor }));
    const [painelDados, metas, listaIndicadores, evolucao] = await Promise.all([
        api.get(`/colaboradores/${id}/dashboard`),
        api.get(`/empresas/${sessao.empresaId}/metas?colaboradorId=${id}`),
        indicadores(),
        sessao.pode('GAMIFICACAO_LER') ? api.get(`/colaboradores/${id}/evolucao`) : Promise.resolve(null)
    ]);
    const porNome = new Map(listaIndicadores.map(i => [i.nome, i]));
    const porId = new Map(listaIndicadores.map(i => [i.indicadorId, i]));

    if (seletor) el.append(seletor);

    el.append(h('div', { class: 'grade cards' },
        stat('Pontos', fmt.num(painelDados.pontosTotais), 'Total acumulado'),
        stat('AI Credits', fmt.num(painelDados.aiCreditsSaldo), 'Saldo disponível'),
        stat('Nível', painelDados.nivelEvolucao || '—',
            evolucao && evolucao.proximoNivel ? `Faltam ${fmt.num(evolucao.pontosParaProximoNivel)} pts para ${evolucao.proximoNivel.nome}` : 'Nível máximo'),
        stat('Conquistas', fmt.num(painelDados.conquistas)),
        stat('Metas em andamento', fmt.num(painelDados.metasEmAndamento)),
        stat('Metas concluídas', fmt.num(painelDados.metasConcluidas))));

    if (evolucao && evolucao.proximoNivel) {
        el.append(painel(`Rumo ao nível ${evolucao.proximoNivel.nome}`, progresso(evolucao.percentualAteProximoNivel),
            { subtitulo: `${fmt.num(evolucao.pontosTotais)} de ${fmt.num(evolucao.proximoNivel.pontosMinimos)} pontos` }));
    }

    // ---- metas
    el.append(painel('Metas', tabela([
        { titulo: 'Meta', valor: m => m.descricao },
        { titulo: 'Prazo', valor: m => fmt.data(m.dataFim) },
        { titulo: 'Progresso', valor: m => h('div', {}, progresso(percentualMeta(m, m.indicadorId ? porId.get(m.indicadorId)?.maiorMelhor !== false : true), { sucesso: m.status === 'Concluida' }),
                h('small', { class: 'muted' }, `${fmt.num(m.progressoAtual)} de ${fmt.num(m.valorAlvo)}`)) },
        { titulo: 'Status', valor: m => status(m.status) }
    ], metas, { vazioTexto: 'Nenhuma meta atribuída.' })));

    // ---- indicadores
    const ultimos = painelDados.indicadores;
    const areaGrafico = h('div', { class: 'grafico' });
    const listaUltimos = ultimos.length === 0 ? vazio('Nenhum indicador registrado.') : h('div', { class: 'lista' },
        ultimos.map(i => h('div', { class: 'lista-item' },
            h('div', {}, h('div', { class: 'titulo' }, i.indicadorNome), h('div', { class: 'sub' }, `Referência ${fmt.mes(i.periodoReferencia)}`)),
            h('div', { class: 'forte' }, fmt.comUnidade(i.ultimoValor, i.unidade)))));

    async function desenhar(nome) {
        const indicador = porNome.get(nome);
        if (!indicador) return;
        const valores = await api.get(`/indicadores/${indicador.indicadorId}/valores?colaboradorId=${id}`);
        grafico(areaGrafico, 'line', {
            labels: valores.map(v => fmt.mes(v.periodoReferencia)),
            datasets: [serie(`${indicador.nome} (${indicador.unidade || ''})`, valores.map(v => Number(v.valor)), 0, { fill: true })]
        });
    }

    const escolha = ultimos.length > 0 ? select(ultimos.map(i => ({ valor: i.indicadorNome, rotulo: i.indicadorNome })),
        { valor: ultimos[0].indicadorNome, aoMudar: desenhar }) : null;

    el.append(h('div', { class: 'grade duas' },
        painel('Últimos indicadores', listaUltimos, { subtitulo: 'Valor mais recente de cada indicador' }),
        painel('Evolução', ultimos.length ? areaGrafico : vazio('Sem histórico.'), { acoes: escolha })));

    if (ultimos.length) await desenhar(ultimos[0].indicadorNome);
}
