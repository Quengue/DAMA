import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { nomes, pessoasAtivas } from '../dados.js';
import { grafico } from '../graficos.js';
import { h, fmt, painel, tabela, badge, botao, formulario, opcoes, toast, stat } from '../ui.js';

export default async function (el, _params, recarregar) {
    const e = sessao.empresaId;
    const escreve = sessao.pode('GAMIFICACAO_ESCREVER');
    const [ranking, minhaPontuacao, regras, n] = await Promise.all([
        api.get(`/empresas/${e}/analytics/ranking-colaboradores`),
        sessao.pode('GAMIFICACAO_LER') ? api.get(`/colaboradores/${sessao.id}/pontuacao`) : Promise.resolve(null),
        api.get(`/empresas/${e}/regras-pontuacao`).catch(() => []),
        nomes()
    ]);
    const eu = ranking.find(r => r.colaboradorId === sessao.id);

    el.append(h('div', { class: 'grade cards' },
        stat('Minha posição', eu ? `${eu.posicao}º` : '—', `de ${ranking.length} colaboradores`),
        stat('Meus pontos', fmt.num(eu?.pontosTotais ?? minhaPontuacao?.pontosTotais)),
        stat('Líder', ranking[0]?.nome || '—', ranking[0] ? `${fmt.num(ranking[0].pontosTotais)} pontos` : null)));

    const areaGrafico = h('div', { class: 'grafico' });
    el.append(h('div', { class: 'grade duas' },
        painel('Ranking de colaboradores', tabela([
            { titulo: '#', valor: r => r.posicao <= 3 ? badge(`${r.posicao}º`, 'acento') : `${r.posicao}º` },
            { titulo: 'Colaborador', valor: r => h('span', { class: r.colaboradorId === sessao.id ? 'forte' : '' }, r.nome) },
            { titulo: 'Equipe', valor: r => n.departamento(r.departamentoId) },
            { titulo: 'Pontos', classe: 'num', valor: r => fmt.num(r.pontosTotais) }
        ], ranking, { destacar: r => r.colaboradorId === sessao.id }), {
            acoes: escreve ? botao('Lançar pontos', () => lancar(regras, recarregar), { tipo: 'primario' }) : null
        }),
        painel('Top 10', areaGrafico)));

    const top = ranking.slice(0, 10);
    grafico(areaGrafico, 'bar', {
        labels: top.map(r => r.nome.split(' ')[0]),
        datasets: [{
            label: 'Pontos', data: top.map(r => Number(r.pontosTotais)),
            backgroundColor: top.map(r => r.colaboradorId === sessao.id ? '#ffa726' : '#ff3b00cc'), borderRadius: 6
        }]
    }, { indexAxis: 'y', plugins: { legend: { display: false } } });

    if (minhaPontuacao) {
        el.append(painel('Meu histórico de pontos', tabela([
            { titulo: 'Data', valor: p => fmt.dataHora(p.registradoEm) },
            { titulo: 'Descrição', valor: p => p.descricao },
            { titulo: 'Pontos', classe: 'num', valor: p => '+' + fmt.num(p.pontos) }
        ], minhaPontuacao.historico, { vazioTexto: 'Nenhum ponto lançado ainda.' })));
    }

    el.append(painel('Regras de pontuação', tabela([
        { titulo: 'Regra', valor: r => r.nome },
        { titulo: 'Pontos', classe: 'num', valor: r => fmt.num(r.pontos) },
        { titulo: 'Situação', valor: r => badge(r.ativa ? 'Ativa' : 'Inativa', r.ativa ? 'sucesso' : '') },
        escreve && {
            titulo: '', valor: r => botao(r.ativa ? 'Desativar' : 'Ativar', async () => {
                try {
                    await api.patch(`/regras-pontuacao/${r.regraId}/ativa`, { ativa: !r.ativa });
                    recarregar();
                } catch (err) {
                    toast(err.message, 'erro');
                }
            }, { pequeno: true })
        }
    ].filter(Boolean), regras, { vazioTexto: 'Nenhuma regra cadastrada.' }), {
        subtitulo: 'Pontos só podem ser lançados a partir de uma regra ativa',
        acoes: escreve ? botao('Nova regra', () => novaRegra(recarregar)) : null
    }));
}

async function lancar(regras, recarregar) {
    const ativas = regras.filter(r => r.ativa);
    formulario('Lançar pontos', [
        { nome: 'colaboradorId', rotulo: 'Colaborador', tipo: 'select', obrigatorio: true, opcoes: opcoes(await pessoasAtivas(), p => p.colaboradorId, p => p.nome) },
        { nome: 'regraId', rotulo: 'Regra', tipo: 'select', obrigatorio: true, opcoes: opcoes(ativas, r => r.regraId, r => `${r.nome} (+${fmt.num(r.pontos)})`) },
        { nome: 'descricao', rotulo: 'Descrição', inteiro: true, ajuda: 'Opcional; se vazio, usa o nome da regra.' }
    ], async v => {
        await api.post(`/colaboradores/${v.colaboradorId}/pontuacao`, { regraId: Number(v.regraId), descricao: v.descricao });
        toast('Pontos lançados.');
        recarregar();
    }, { textoBotao: 'Lançar' });
}

function novaRegra(recarregar) {
    formulario('Nova regra de pontuação', [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true },
        { nome: 'pontos', rotulo: 'Pontos', tipo: 'number', obrigatorio: true }
    ], async v => {
        await api.post(`/empresas/${sessao.empresaId}/regras-pontuacao`, v);
        toast('Regra criada.');
        recarregar();
    });
}
