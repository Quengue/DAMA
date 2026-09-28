import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { nomes } from '../dados.js';
import { h, fmt, painel, tabela, badge, select } from '../ui.js';
import { navegar } from '../app.js';

export default async function (el, params) {
    const entidade = params.get('entidade') || '';
    const [eventos, todos, n] = await Promise.all([
        api.get(`/empresas/${sessao.empresaId}/auditoria${entidade ? `?entidade=${encodeURIComponent(entidade)}` : ''}`),
        entidade ? api.get(`/empresas/${sessao.empresaId}/auditoria`) : Promise.resolve(null),
        nomes()
    ]);
    const entidades = [...new Set((todos || eventos).map(ev => ev.entidade))].sort();

    el.append(painel(`Eventos (${eventos.length})`, tabela([
        { titulo: 'Quando', valor: ev => fmt.dataHora(ev.registradoEm) },
        { titulo: 'Quem', valor: ev => ev.colaboradorId ? n.pessoa(ev.colaboradorId) : 'Sistema' },
        { titulo: 'Entidade', valor: ev => badge(ev.entidade) },
        { titulo: 'Registro', valor: ev => h('code', {}, ev.entidadeId) },
        { titulo: 'Ação', valor: ev => ev.acao }
    ], eventos.slice(0, 300), { vazioTexto: 'Nenhum evento registrado.' }), {
        subtitulo: 'Mais recentes primeiro (até 300)',
        acoes: select([{ valor: '', rotulo: 'Todas as entidades' }, ...entidades.map(e => ({ valor: e, rotulo: e }))],
            { valor: entidade, aoMudar: v => navegar('auditoria', v ? { entidade: v } : undefined) })
    }));
}
