import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { nomes, pessoasAtivas } from '../dados.js';
import { h, fmt, painel, badge, botao, formulario, opcoes, select, toast, vazio } from '../ui.js';
import { navegar } from '../app.js';

const TIPOS = ['Destaque', 'Colaboração', 'Inovação', 'Mentoria', 'Atendimento ao cliente'];

export default async function (el, params, recarregar) {
    const colaboradorId = Number(params.get('colaborador')) || null;
    const [lista, n, pessoas] = await Promise.all([
        colaboradorId ? api.get(`/colaboradores/${colaboradorId}/reconhecimentos`)
            : api.get(`/empresas/${sessao.empresaId}/reconhecimentos`),
        nomes(),
        pessoasAtivas()
    ]);

    el.append(painel('Reconhecimentos', lista.length === 0 ? vazio('Nenhum reconhecimento registrado.') :
        h('div', { class: 'lista' }, lista.map(r => h('div', { class: 'lista-item' },
            h('div', {},
                h('div', { class: 'linha' }, h('span', { class: 'titulo' }, n.pessoa(r.colaboradorId)), badge(r.tipo, 'acento')),
                h('div', { style: 'margin-top:4px' }, r.descricao),
                h('div', { class: 'sub' }, `Por ${n.pessoa(r.concedidoPorId)} · ${fmt.data(r.registradoEm)}`))))), {
        acoes: [
            select(opcoes(pessoas, p => p.colaboradorId, p => p.nome, { vazioRotulo: 'Toda a empresa' }),
                { valor: colaboradorId || '', aoMudar: v => navegar('reconhecimentos', v ? { colaborador: v } : undefined) }),
            sessao.pode('GAMIFICACAO_ESCREVER') && botao('Reconhecer', () => reconhecer(pessoas, recarregar), { tipo: 'primario' })
        ]
    }));
}

function reconhecer(pessoas, recarregar) {
    formulario('Reconhecer colaborador', [
        { nome: 'colaboradorId', rotulo: 'Colaborador', tipo: 'select', obrigatorio: true, opcoes: opcoes(pessoas.filter(p => p.colaboradorId !== sessao.id), p => p.colaboradorId, p => p.nome) },
        { nome: 'tipo', rotulo: 'Tipo', tipo: 'select', obrigatorio: true, opcoes: TIPOS.map(t => ({ valor: t, rotulo: t })) },
        { nome: 'descricao', rotulo: 'Motivo', tipo: 'textarea', obrigatorio: true }
    ], async v => {
        await api.post(`/empresas/${sessao.empresaId}/reconhecimentos`, { ...v, colaboradorId: Number(v.colaboradorId) });
        toast('Reconhecimento registrado.');
        recarregar();
    }, { textoBotao: 'Reconhecer' });
}
