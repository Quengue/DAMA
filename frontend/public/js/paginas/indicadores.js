import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { indicadores, nomes, limparCache } from '../dados.js';
import { grafico, serie } from '../graficos.js';
import { h, fmt, painel, tabela, badge, botao, formulario, opcoes, select, toast, vazio } from '../ui.js';
import { opcoesResponsavel, lerResponsavel } from './comum.js';
import { navegar } from '../app.js';

const MAX_SERIES = 8;

export default async function (el, params, recarregar) {
    const escreve = sessao.pode('DESEMPENHO_ESCREVER');
    const [lista, n, responsaveis] = await Promise.all([indicadores(), nomes(), opcoesResponsavel()]);

    const acoes = escreve ? [
        botao('Registrar valor', () => registrarValor(lista, responsaveis, recarregar), { tipo: 'primario' }),
        botao('Novo indicador', () => novoIndicador(recarregar))
    ] : null;

    if (lista.length === 0) {
        el.append(painel('Indicadores', vazio('Nenhum indicador cadastrado.'), { acoes }));
        return;
    }

    const indicadorId = Number(params.get('ind')) || lista[0].indicadorId;
    const indicador = lista.find(i => i.indicadorId === indicadorId) || lista[0];
    const inicio = params.get('inicio') || '';
    const fim = params.get('fim') || '';

    const consulta = new URLSearchParams();
    if (inicio) consulta.set('inicio', `${inicio}-01`);
    if (fim) consulta.set('fim', `${fim}-01`);
    const recorteEscolhido = params.get('serie');
    if (recorteEscolhido && recorteEscolhido.includes(':')) {
        const r = lerResponsavel(recorteEscolhido);
        if (r.colaboradorId) consulta.set('colaboradorId', r.colaboradorId);
        if (r.departamentoId) consulta.set('departamentoId', r.departamentoId);
    }
    const todos = await api.get(`/indicadores/${indicador.indicadorId}/valores?${consulta}`);
    // sem escolha explícita, mostra as equipes quando o indicador tem valores por equipe; senão, as pessoas
    const recorte = recorteEscolhido || (todos.some(v => v.departamentoId != null) ? 'equipes' : 'pessoas');
    const valores = todos.filter(v => recorte === 'equipes' ? v.departamentoId != null
        : recorte === 'pessoas' ? v.colaboradorId != null : true);

    const filtrar = (chave, valor) => {
        const novo = Object.fromEntries(params);
        novo.ind = indicador.indicadorId;
        if (valor) novo[chave] = valor; else delete novo[chave];
        navegar('indicadores', novo);
    };
    const campoMes = (chave, valor, rotulo) => h('label', { class: 'campo' }, h('span', { class: 'muted', style: 'font-size:11px' }, rotulo),
        h('input', { type: 'month', class: 'filtro', value: valor, onchange: ev => filtrar(chave, ev.target.value) }));

    el.append(painel(null, h('div', { class: 'filtros' },
        h('label', { class: 'campo' }, h('span', { class: 'muted', style: 'font-size:11px' }, 'Indicador'),
            select(lista.map(i => ({ valor: i.indicadorId, rotulo: i.nome })), { valor: indicador.indicadorId, aoMudar: v => navegar('indicadores', { ind: v }) })),
        h('label', { class: 'campo' }, h('span', { class: 'muted', style: 'font-size:11px' }, 'Série'),
            select([{ valor: 'equipes', rotulo: 'Todas as equipes' }, { valor: 'pessoas', rotulo: 'Todas as pessoas' }, ...responsaveis],
                { valor: recorte, aoMudar: v => filtrar('serie', v) })),
        campoMes('inicio', inicio, 'De'),
        campoMes('fim', fim, 'Até'),
        (inicio || fim) && botao('Limpar período', () => navegar('indicadores', { ind: indicador.indicadorId, serie: recorte }))),
        { acoes }));

    // uma linha por colaborador/equipe
    const series = new Map();
    for (const v of valores) {
        const chave = v.colaboradorId ? `c:${v.colaboradorId}` : `d:${v.departamentoId}`;
        if (!series.has(chave)) series.set(chave, { nome: v.colaboradorId ? n.pessoa(v.colaboradorId) : `Equipe ${n.departamento(v.departamentoId)}`, valores: new Map() });
        series.get(chave).valores.set(v.periodoReferencia, Number(v.valor));
    }
    const periodos = [...new Set(valores.map(v => v.periodoReferencia))].sort();
    const exibidas = [...series.values()].slice(0, MAX_SERIES);

    const area = h('div', { class: 'grafico' });
    el.append(painel(`${indicador.nome}${indicador.unidade ? ` (${indicador.unidade})` : ''}`,
        valores.length ? area : vazio('Nenhum valor no recorte escolhido.'), {
            subtitulo: [indicador.descricao, indicador.maiorMelhor ? 'Maior é melhor' : 'Menor é melhor',
                series.size > MAX_SERIES ? `Mostrando ${MAX_SERIES} de ${series.size} séries` : null].filter(Boolean).join(' · ')
        }));
    if (valores.length) {
        grafico(area, 'line', {
            labels: periodos.map(fmt.mes),
            datasets: exibidas.map((s, i) => serie(s.nome, periodos.map(p => s.valores.get(p) ?? null), i, { spanGaps: true }))
        });
    }

    el.append(h('div', { class: 'grade duas' },
        painel('Valores', tabela([
            { titulo: 'Período', valor: v => fmt.mes(v.periodoReferencia) },
            { titulo: 'Série', valor: v => v.colaboradorId ? n.pessoa(v.colaboradorId) : `Equipe ${n.departamento(v.departamentoId)}` },
            { titulo: 'Valor', classe: 'num', valor: v => fmt.num(v.valor) }
        ], [...valores].reverse().slice(0, 60), { vazioTexto: 'Sem valores.' })),
        painel('Indicadores da empresa', tabela([
            { titulo: 'Indicador', valor: i => h('a', { href: `#/indicadores?ind=${i.indicadorId}` }, i.nome) },
            { titulo: 'Unidade', valor: i => i.unidade || '—' },
            { titulo: 'Tipo', valor: i => badge(i.maiorMelhor ? 'Maior é melhor' : 'Menor é melhor', i.maiorMelhor ? 'info' : 'alerta') }
        ], lista))));
}

function novoIndicador(recarregar) {
    formulario('Novo indicador', [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true },
        { nome: 'unidade', rotulo: 'Unidade', ajuda: 'Ex.: %, tarefas/mês, NPS' },
        { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea' },
        { nome: 'maiorMelhor', rotulo: 'Quanto maior, melhor', tipo: 'checkbox', valor: true, inteiro: true, ajuda: 'Desmarque para indicadores como retrabalho ou tempo de resposta.' }
    ], async v => {
        await api.post(`/empresas/${sessao.empresaId}/indicadores`, v);
        limparCache();
        toast('Indicador criado.');
        recarregar();
    });
}

function registrarValor(lista, responsaveis, recarregar) {
    const mesAtual = new Date().toISOString().slice(0, 7);
    formulario('Registrar valor de indicador', [
        { nome: 'indicadorId', rotulo: 'Indicador', tipo: 'select', obrigatorio: true, opcoes: opcoes(lista, i => i.indicadorId, i => i.nome) },
        { nome: 'responsavel', rotulo: 'Colaborador ou equipe', tipo: 'select', obrigatorio: true, opcoes: responsaveis },
        { nome: 'periodo', rotulo: 'Mês de referência', tipo: 'month', obrigatorio: true, valor: mesAtual },
        { nome: 'valor', rotulo: 'Valor', tipo: 'number', obrigatorio: true }
    ], async v => {
        await api.post(`/indicadores/${v.indicadorId}/valores`, {
            ...lerResponsavel(v.responsavel), periodoReferencia: `${v.periodo}-01`, valor: v.valor
        });
        toast('Valor registrado.');
        recarregar();
    }, { textoBotao: 'Registrar' });
}
