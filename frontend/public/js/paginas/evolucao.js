import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { pessoasAtivas } from '../dados.js';
import { h, fmt, painel, stat, tabela, badge, progresso, botao, formulario, opcoes, toast, vazio } from '../ui.js';
import { seletorPessoa } from './comum.js';
import { navegar } from '../app.js';

export default async function (el, params, recarregar) {
    const { elemento: seletor, id } = await seletorPessoa(params, valor => navegar('evolucao', { colaborador: valor }),
        'GAMIFICACAO_ESCREVER');
    const [evolucao, niveis, catalogo, criterios] = await Promise.all([
        api.get(`/colaboradores/${id}/evolucao`),
        api.get('/niveis-evolucao'),
        api.get(`/empresas/${sessao.empresaId}/conquistas`),
        api.get('/conquistas/criterios')
    ]);
    const descricaoCriterio = new Map(criterios.map(c => [c.codigo, c.descricao]));
    const obtidas = new Map(evolucao.conquistas.map(c => [c.conquistaId, c]));

    if (seletor) el.append(seletor);

    el.append(h('div', { class: 'grade cards' },
        stat('Nível atual', evolucao.nivelAtual?.nome || '—', evolucao.nivelAtual?.descricao),
        stat('Pontos', fmt.num(evolucao.pontosTotais)),
        stat('Próximo nível', evolucao.proximoNivel?.nome || 'Nível máximo',
            evolucao.proximoNivel ? `Faltam ${fmt.num(evolucao.pontosParaProximoNivel)} pontos` : null),
        stat('Conquistas', `${evolucao.conquistas.length} de ${catalogo.filter(c => c.ativa).length}`)));

    el.append(painel('Níveis de evolução', [
        h('div', { class: 'niveis' }, niveis.map(n => h('div', {
            class: 'nivel' + (Number(evolucao.pontosTotais) >= Number(n.pontosMinimos) ? ' atingido' : '')
                + (evolucao.nivelAtual?.nivelId === n.nivelId ? ' atual' : '')
        }, h('div', { class: 'n' }, n.nome), h('div', { class: 'm' }, `${fmt.num(n.pontosMinimos)} pts`)))),
        evolucao.proximoNivel && h('div', { style: 'margin-top:14px' }, progresso(evolucao.percentualAteProximoNivel))
    ], { subtitulo: 'O nível sobe conforme os pontos acumulados' }));

    const ativas = catalogo.filter(c => c.ativa);
    el.append(painel('Conquistas', ativas.length === 0 ? vazio('A empresa ainda não definiu conquistas.') :
        h('div', { class: 'conquistas' }, ativas
            .sort((a, b) => (obtidas.has(b.conquistaId) ? 1 : 0) - (obtidas.has(a.conquistaId) ? 1 : 0))
            .map(c => {
                const obtida = obtidas.get(c.conquistaId);
                return h('div', { class: 'conquista' + (obtida ? '' : ' bloqueada') },
                    h('div', { class: 'medalha' }, obtida ? '★' : '?'),
                    h('div', {},
                        h('div', { class: 'nome' }, c.nome),
                        h('div', { class: 'desc' }, c.descricao || ''),
                        h('div', { class: 'desc' }, obtida
                            ? `Obtida em ${fmt.data(obtida.obtidaEm)}${obtida.concedidaPorId ? ' (concedida por gestor)' : ''}`
                            : requisito(c, descricaoCriterio))));
            })),
        { subtitulo: 'Concedidas automaticamente quando o critério é atingido' }));

    if (sessao.pode('GAMIFICACAO_ESCREVER')) {
        el.append(await painelGestao(catalogo, criterios, descricaoCriterio, recarregar));
    }
}

function requisito(conquista, descricaoCriterio) {
    if (conquista.criterio === 'MANUAL') return 'Concedida por um gestor';
    return `${descricaoCriterio.get(conquista.criterio) || conquista.criterio}: ${fmt.num(conquista.valorMinimo)}`;
}

async function painelGestao(catalogo, criterios, descricaoCriterio, recarregar) {
    const e = sessao.empresaId;

    const nova = () => formulario('Nova conquista', [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true },
        { nome: 'criterio', rotulo: 'Critério', tipo: 'select', obrigatorio: true, opcoes: opcoes(criterios, c => c.codigo, c => c.descricao) },
        { nome: 'valorMinimo', rotulo: 'Valor mínimo', tipo: 'number', ajuda: 'Obrigatório nos critérios automáticos; deixe vazio em "Concedida manualmente".' },
        { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea' }
    ], async v => {
        const criada = await api.post(`/empresas/${e}/conquistas`, v);
        toast(`Conquista criada. ${criada.colaboradoresQueObtiveram} colaborador(es) já a receberam.`);
        recarregar();
    });

    const manuais = catalogo.filter(c => c.criterio === 'MANUAL' && c.ativa);
    const conceder = async () => formulario('Conceder conquista', [
        { nome: 'colaboradorId', rotulo: 'Colaborador', tipo: 'select', obrigatorio: true, opcoes: opcoes(await pessoasAtivas(), p => p.colaboradorId, p => p.nome) },
        { nome: 'conquistaId', rotulo: 'Conquista', tipo: 'select', obrigatorio: true, opcoes: opcoes(manuais, c => c.conquistaId, c => c.nome) }
    ], async v => {
        await api.post(`/colaboradores/${v.colaboradorId}/conquistas`, { conquistaId: Number(v.conquistaId) });
        toast('Conquista concedida.');
        recarregar();
    }, { textoBotao: 'Conceder' });

    const reavaliar = async () => {
        const r = await api.post(`/empresas/${e}/conquistas/reavaliar`);
        toast(r.concedidas ? `${r.concedidas} conquista(s) concedida(s).` : 'Nenhuma conquista nova.');
        recarregar();
    };

    const alternar = async c => {
        await api.patch(`/conquistas/${c.conquistaId}/ativa`, { ativa: !c.ativa });
        toast(c.ativa ? 'Conquista desativada.' : 'Conquista ativada.');
        recarregar();
    };

    return painel('Catálogo de conquistas da empresa', tabela([
        { titulo: 'Conquista', valor: c => h('div', {}, h('div', { class: 'forte' }, c.nome), h('small', { class: 'muted' }, c.descricao || '')) },
        { titulo: 'Critério', valor: c => requisito(c, descricaoCriterio) },
        { titulo: 'Obtida por', classe: 'num', valor: c => fmt.num(c.colaboradoresQueObtiveram) },
        { titulo: 'Situação', valor: c => badge(c.ativa ? 'Ativa' : 'Inativa', c.ativa ? 'sucesso' : '') },
        { titulo: '', valor: c => botao(c.ativa ? 'Desativar' : 'Ativar', () => alternar(c).catch(err => toast(err.message, 'erro')), { pequeno: true }) }
    ], catalogo, { vazioTexto: 'Nenhuma conquista cadastrada.' }), {
        subtitulo: 'Visível para quem gerencia a gamificação',
        acoes: [
            botao('Reavaliar todos', () => reavaliar().catch(err => toast(err.message, 'erro'))),
            manuais.length > 0 && botao('Conceder manual', () => conceder()),
            botao('Nova conquista', nova, { tipo: 'primario' })
        ]
    });
}
