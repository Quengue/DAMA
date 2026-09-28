import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { h, fmt, painel, stat, badge, select, vazio } from '../ui.js';
import { navegar } from '../app.js';

const TIPOS = {
    META_VENCIDA: 'Meta vencida',
    META_EM_RISCO: 'Meta em risco',
    INDICADOR_EM_QUEDA: 'Indicador em queda',
    EQUIPE_ABAIXO_DA_MEDIA: 'Equipe abaixo da média',
    SEM_ATIVIDADE_RECENTE: 'Sem atividade recente'
};
const SEVERIDADE = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };

export default async function (el, params) {
    const e = sessao.empresaId;
    const [visao, analise] = await Promise.all([
        api.get(`/empresas/${e}/analytics/visao-consolidada`),
        api.get(`/empresas/${e}/analytics/alertas`)
    ]);
    const filtroTipo = params.get('tipo') || '';
    const filtroSeveridade = params.get('severidade') || '';
    const alertas = analise.alertas.filter(a => (!filtroTipo || a.tipo === filtroTipo) && (!filtroSeveridade || a.severidade === filtroSeveridade));

    el.append(h('div', { class: 'grade cards' },
        stat('Colaboradores ativos', fmt.num(visao.colaboradoresAtivos), `${fmt.num(visao.totalDepartamentos)} equipes`),
        stat('Metas em andamento', fmt.num(visao.metasEmAndamento), `${fmt.num(visao.metasConcluidas)} concluídas`),
        stat('Pontos na empresa', fmt.num(visao.pontosTotaisPeriodo)),
        stat('Alertas de prioridade alta', fmt.num(analise.totalPorSeveridade.ALTA),
            `${fmt.num(analise.totalPorSeveridade.MEDIA)} média · ${fmt.num(analise.totalPorSeveridade.BAIXA)} baixa`)));

    const filtrar = (chave, valor) => {
        const novo = Object.fromEntries(params);
        if (valor) novo[chave] = valor; else delete novo[chave];
        navegar('analises', novo);
    };

    el.append(painel('Onde agir', alertas.length === 0 ? vazio('Nenhum ponto de atenção no momento.') :
        h('div', { class: 'lista', style: 'gap:10px' }, alertas.map(a => h('div', { class: 'alerta-item ' + a.severidade },
            h('div', { class: 'linha', style: 'justify-content:space-between' },
                h('div', { class: 'titulo' }, a.titulo),
                h('div', { class: 'linha' }, badge(TIPOS[a.tipo] || a.tipo), badge(SEVERIDADE[a.severidade], a.severidade === 'ALTA' ? 'perigo' : a.severidade === 'MEDIA' ? 'alerta' : 'info'))),
            h('div', { class: 'detalhe' }, a.detalhe),
            h('div', { class: 'recomendacao' }, a.recomendacao),
            h('div', { class: 'linha', style: 'margin-top:8px' }, links(a))))), {
        subtitulo: `Gerado em ${fmt.data(analise.dataReferencia)} a partir dos dados registrados na plataforma`,
        acoes: [
            select([{ valor: '', rotulo: 'Todas as severidades' }, ...Object.entries(SEVERIDADE).map(([valor, rotulo]) => ({ valor, rotulo }))],
                { valor: filtroSeveridade, aoMudar: v => filtrar('severidade', v) }),
            select([{ valor: '', rotulo: 'Todos os tipos' }, ...Object.entries(TIPOS).map(([valor, rotulo]) => ({ valor, rotulo }))],
                { valor: filtroTipo, aoMudar: v => filtrar('tipo', v) })
        ]
    }));
}

function links(alerta) {
    const itens = [];
    if (alerta.tipo.startsWith('META')) {
        const responsavel = alerta.colaboradorId ? `c:${alerta.colaboradorId}` : `d:${alerta.departamentoId}`;
        itens.push(h('a', { href: `#/metas?responsavel=${responsavel}` }, 'Ver metas'));
    }
    if (alerta.tipo === 'INDICADOR_EM_QUEDA') {
        const serie = alerta.colaboradorId ? `c:${alerta.colaboradorId}` : `d:${alerta.departamentoId}`;
        itens.push(h('a', { href: `#/indicadores?ind=${alerta.referenciaId}&serie=${serie}` }, 'Ver evolução do indicador'));
    }
    if (alerta.departamentoId) {
        itens.push(h('a', { href: `#/equipes?dep=${alerta.departamentoId}` }, 'Ver equipe'));
    }
    if (alerta.colaboradorId) {
        itens.push(h('a', { href: `#/inicio?colaborador=${alerta.colaboradorId}` }, 'Ver painel do colaborador'));
    }
    return itens.flatMap((link, i) => i === 0 ? [link] : [h('span', { class: 'muted' }, '·'), link]);
}
