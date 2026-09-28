import { api } from '../api.js';
import { sessao } from '../sessao.js';
import { departamentos, limparCache } from '../dados.js';
import { h, fmt, painel, tabela, badge, botao, formulario, opcoes, toast, modal, confirmar, select, hojeIso } from '../ui.js';

const SENIORIDADES = ['Junior', 'Pleno', 'Senior', 'Especialista', 'Gerente'];

export default async function (el, _params, recarregar) {
    const e = sessao.empresaId;
    const escreve = sessao.pode('COLABORADOR_ESCREVER');
    const gerenciaPerfis = sessao.pode('PERFIL_GERENCIAR');
    const [pessoas, equipes, perfis] = await Promise.all([
        api.get(`/empresas/${e}/colaboradores`),
        departamentos(),
        gerenciaPerfis ? api.get('/perfis') : Promise.resolve([])
    ]);
    const atualizar = () => { limparCache(); recarregar(); };

    el.append(painel(`Colaboradores (${pessoas.filter(p => p.ativo).length} ativos)`, tabela([
        { titulo: 'Nome', valor: p => h('div', {}, h('div', { class: 'forte' }, p.nome), h('small', { class: 'muted' }, p.email)) },
        { titulo: 'Cargo', valor: p => h('div', {}, p.cargo || '—', h('div', { class: 'muted', style: 'font-size:12px' }, p.senioridade || '')) },
        { titulo: 'Equipe', valor: p => p.departamentoNome },
        { titulo: 'Admissão', valor: p => fmt.data(p.dataAdmissao) },
        { titulo: 'Situação', valor: p => badge(p.ativo ? 'Ativo' : 'Desligado', p.ativo ? 'sucesso' : '') },
        {
            titulo: '', valor: p => h('div', { class: 'linha' },
                escreve && botao('Editar', () => editar(p, equipes, atualizar), { pequeno: true }),
                gerenciaPerfis && botao('Perfis', () => gerenciarPerfis(p, perfis), { pequeno: true }),
                escreve && botao(p.ativo ? 'Desligar' : 'Reativar', () => confirmar(
                    p.ativo ? `Desligar ${p.nome}? O registro e o histórico são mantidos.` : `Reativar ${p.nome}?`,
                    async () => {
                        await api.patch(`/colaboradores/${p.colaboradorId}/status`, { ativo: !p.ativo });
                        toast(p.ativo ? 'Colaborador desligado.' : 'Colaborador reativado.');
                        atualizar();
                    }), { pequeno: true, tipo: p.ativo ? 'perigo' : '' }))
        }
    ], pessoas), {
        acoes: [
            gerenciaPerfis && botao('Nova equipe', () => novaEquipe(atualizar)),
            escreve && botao('Novo colaborador', () => novo(equipes, atualizar), { tipo: 'primario' })
        ]
    }));

    if (gerenciaPerfis) {
        el.append(painel('Perfis de acesso', tabela([
            { titulo: 'Perfil', valor: p => h('div', {}, h('div', { class: 'forte' }, p.nome), h('small', { class: 'muted' }, p.descricao || '')) },
            { titulo: 'Permissões', valor: p => h('div', { class: 'linha' }, p.permissoes.map(c => badge(c))) }
        ], perfis), { acoes: botao('Novo perfil', () => novoPerfil(recarregar)) }));
    }
}

function camposColaborador(equipes, p = {}) {
    return [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true, valor: p.nome },
        { nome: 'email', rotulo: 'E-mail', tipo: 'email', obrigatorio: true, valor: p.email },
        { nome: 'departamentoId', rotulo: 'Equipe', tipo: 'select', obrigatorio: true, valor: p.departamentoId, opcoes: opcoes(equipes, d => d.departamentoId, d => d.nome) },
        { nome: 'cargo', rotulo: 'Cargo', valor: p.cargo },
        { nome: 'senioridade', rotulo: 'Senioridade', tipo: 'select', valor: p.senioridade || '', opcoes: [{ valor: '', rotulo: '—' }, ...SENIORIDADES.map(s => ({ valor: s, rotulo: s }))] }
    ];
}

function novo(equipes, atualizar) {
    formulario('Novo colaborador', [
        ...camposColaborador(equipes),
        { nome: 'dataAdmissao', rotulo: 'Data de admissão', tipo: 'date', obrigatorio: true, valor: hojeIso() }
    ], async v => {
        await api.post(`/empresas/${sessao.empresaId}/colaboradores`, { ...v, departamentoId: Number(v.departamentoId) });
        toast('Colaborador cadastrado.');
        atualizar();
    });
}

function editar(p, equipes, atualizar) {
    formulario(`Editar ${p.nome}`, camposColaborador(equipes, p), async v => {
        await api.put(`/colaboradores/${p.colaboradorId}`, { ...v, departamentoId: Number(v.departamentoId) });
        toast('Dados atualizados.');
        atualizar();
    });
}

function novaEquipe(atualizar) {
    formulario('Nova equipe', [{ nome: 'nome', rotulo: 'Nome', obrigatorio: true, inteiro: true }], async v => {
        await api.post(`/empresas/${sessao.empresaId}/departamentos`, v);
        toast('Equipe criada.');
        atualizar();
    });
}

async function novoPerfil(recarregar) {
    const permissoes = await api.get('/permissoes');
    formulario('Novo perfil', [
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true },
        { nome: 'descricao', rotulo: 'Descrição' },
        ...permissoes.map(p => ({ nome: `perm_${p.codigo}`, rotulo: `${p.codigo} — ${p.descricao}`, tipo: 'checkbox', inteiro: true }))
    ], async v => {
        const escolhidas = permissoes.filter(p => v[`perm_${p.codigo}`]).map(p => p.codigo);
        await api.post('/perfis', { nome: v.nome, descricao: v.descricao, permissoes: escolhidas });
        toast('Perfil criado.');
        recarregar();
    });
}

async function gerenciarPerfis(pessoa, todos) {
    let fechar;
    const desenhar = async () => {
        const atuais = await api.get(`/colaboradores/${pessoa.colaboradorId}/perfis`);
        const ids = new Set(atuais.map(p => p.perfilId));
        const escolha = select(opcoes(todos.filter(p => !ids.has(p.perfilId)), p => p.perfilId, p => p.nome, { vazioRotulo: 'Adicionar perfil…' }));
        corpo.replaceChildren(
            tabela([
                { titulo: 'Perfil', valor: p => p.nome },
                {
                    titulo: '', valor: p => botao('Remover', async () => {
                        try {
                            await api.del(`/colaboradores/${pessoa.colaboradorId}/perfis/${p.perfilId}`);
                            await desenhar();
                        } catch (err) {
                            toast(err.message, 'erro');
                        }
                    }, { pequeno: true, tipo: 'perigo' })
                }
            ], atuais, { vazioTexto: 'Sem perfis: este colaborador não consegue usar a plataforma.' }),
            h('div', { class: 'linha', style: 'margin-top:12px' }, escolha, botao('Adicionar', async () => {
                if (!escolha.value) return;
                try {
                    await api.post(`/colaboradores/${pessoa.colaboradorId}/perfis`, { perfilId: Number(escolha.value) });
                    await desenhar();
                } catch (err) {
                    toast(err.message, 'erro');
                }
            }, { tipo: 'primario' })));
    };
    const corpo = h('div', {});
    fechar = modal(`Perfis de ${pessoa.nome}`, corpo, close => h('button', { type: 'button', class: 'btn', onclick: close }, 'Fechar'));
    await desenhar();
    return fechar;
}
