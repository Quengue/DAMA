// Componentes de interface. Todo texto entra via textContent (h), nunca como HTML cru.

export function h(tag, atributos = {}, ...filhos) {
    const el = document.createElement(tag);
    for (const [chave, valor] of Object.entries(atributos || {})) {
        if (valor === undefined || valor === null || valor === false) continue;
        if (chave === 'class') el.className = valor;
        else if (chave === 'style') el.setAttribute('style', valor);
        else if (chave.startsWith('on') && typeof valor === 'function') el.addEventListener(chave.slice(2), valor);
        else if (valor === true) el.setAttribute(chave, '');
        else el.setAttribute(chave, valor);
    }
    for (const filho of filhos.flat(Infinity)) {
        if (filho === undefined || filho === null || filho === false) continue;
        el.append(filho instanceof Node ? filho : document.createTextNode(String(filho)));
    }
    return el;
}

// ---------- formatação ----------

const NUMERO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });

export const fmt = {
    num(valor) {
        return valor === null || valor === undefined || valor === '' ? '—' : NUMERO.format(Number(valor));
    },
    data(iso) {
        if (!iso) return '—';
        const [a, m, d] = String(iso).slice(0, 10).split('-');
        return `${d}/${m}/${a}`;
    },
    dataHora(iso) {
        if (!iso) return '—';
        const data = new Date(iso);
        return data.toLocaleDateString('pt-BR') + ' ' + data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    },
    mes(iso) {
        if (!iso) return '—';
        const [a, m] = String(iso).split('-');
        return `${m}/${a}`;
    },
    comUnidade(valor, unidade) {
        if (!unidade) return fmt.num(valor);
        return fmt.num(valor) + (unidade === '%' ? '%' : ' ' + unidade);
    },
    iniciais(nome) {
        return (nome || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0].toUpperCase()).join('');
    }
};

export function hojeIso(deslocamentoDias = 0) {
    const d = new Date();
    d.setDate(d.getDate() + deslocamentoDias);
    return d.toISOString().slice(0, 10);
}

// ---------- blocos ----------

export function painel(titulo, conteudo, { subtitulo, acoes, classe } = {}) {
    return h('section', { class: 'painel' + (classe ? ' ' + classe : '') },
        (titulo || acoes) && h('div', { class: 'painel-cabecalho' },
            h('div', {}, titulo && h('h3', {}, titulo), subtitulo && h('small', {}, subtitulo)),
            acoes && h('div', { class: 'painel-acoes' }, acoes)),
        conteudo);
}

export function stat(rotulo, valor, detalhe) {
    return h('div', { class: 'painel stat' },
        h('div', { class: 'rotulo' }, rotulo),
        h('div', { class: 'valor' }, valor),
        detalhe && h('div', { class: 'detalhe' }, detalhe));
}

export function badge(texto, tipo = '') {
    return h('span', { class: 'badge ' + tipo }, texto);
}

const TIPO_STATUS = {
    'Em andamento': 'info', 'Concluida': 'sucesso', 'Concluido': 'sucesso', 'Atrasada': 'perigo', 'Cancelada': '',
    'Cancelado': '', 'Planejado': 'acento', 'Encerrado': '', 'Inscrito': 'info', 'Desistente': '',
    'ALTA': 'perigo', 'MEDIA': 'alerta', 'BAIXA': 'info', 'CREDITO': 'sucesso', 'DEBITO': 'perigo'
};
const ROTULO_STATUS = { 'Concluida': 'Concluída', 'Concluido': 'Concluído', 'MEDIA': 'MÉDIA', 'CREDITO': 'Crédito', 'DEBITO': 'Débito' };

export function status(valor) {
    return badge(ROTULO_STATUS[valor] || valor, TIPO_STATUS[valor] || '');
}

export function progresso(percentual, { sucesso = false } = {}) {
    const p = Math.max(0, Math.min(100, Math.round(percentual || 0)));
    return h('div', { class: 'progresso' },
        h('div', { class: 'barra' + (sucesso ? ' sucesso' : ''), style: 'flex:1' }, h('div', { style: `width:${p}%` })),
        h('span', {}, p + '%'));
}

export function vazio(texto) {
    return h('div', { class: 'vazio' }, texto);
}

/** colunas: [{ titulo, valor: linha => conteúdo, classe }] */
export function tabela(colunas, linhas, { destacar, vazioTexto = 'Nenhum registro.' } = {}) {
    if (!linhas || linhas.length === 0) return vazio(vazioTexto);
    return h('div', { class: 'tabela-wrap' },
        h('table', {},
            h('thead', {}, h('tr', {}, colunas.map(c => h('th', { class: c.classe }, c.titulo)))),
            h('tbody', {}, linhas.map(linha =>
                h('tr', { class: destacar && destacar(linha) ? 'destaque' : null },
                    colunas.map(c => h('td', { class: c.classe }, c.valor(linha))))))));
}

export function botao(texto, aoClicar, { tipo = '', pequeno = false, desabilitado = false } = {}) {
    return h('button', {
        type: 'button', class: `btn ${tipo}${pequeno ? ' pequeno' : ''}`, disabled: desabilitado,
        onclick: aoClicar
    }, texto);
}

export function opcoes(lista, valor, rotulo, { vazioRotulo } = {}) {
    const itens = vazioRotulo !== undefined ? [{ valor: '', rotulo: vazioRotulo }] : [];
    for (const item of lista) itens.push({ valor: String(valor(item)), rotulo: rotulo(item) });
    return itens;
}

export function select(itens, { valor = '', aoMudar, classe = 'filtro' } = {}) {
    const el = h('select', { class: classe, onchange: e => aoMudar && aoMudar(e.target.value) },
        itens.map(i => h('option', { value: i.valor }, i.rotulo)));
    el.value = String(valor ?? '');
    return el;
}

// ---------- toast ----------

export function toast(mensagem, tipo = 'ok') {
    const el = h('div', { class: 'toast' + (tipo === 'erro' ? ' erro' : '') }, mensagem);
    document.getElementById('toasts').append(el);
    setTimeout(() => el.remove(), tipo === 'erro' ? 6000 : 3500);
}

// ---------- modal e formulários ----------

export function modal(titulo, conteudo, rodape) {
    const fundo = h('div', { class: 'modal-fundo' });
    const fechar = () => fundo.remove();
    fundo.addEventListener('click', e => { if (e.target === fundo) fechar(); });
    document.addEventListener('keydown', function esc(e) {
        if (e.key === 'Escape') { fechar(); document.removeEventListener('keydown', esc); }
    });
    fundo.append(h('div', { class: 'painel modal' },
        h('div', { class: 'painel-cabecalho' }, h('h3', {}, titulo)),
        conteudo,
        rodape && h('div', { class: 'rodape' }, typeof rodape === 'function' ? rodape(fechar) : rodape)));
    document.body.append(fundo);
    return fechar;
}

/**
 * campos: [{ nome, rotulo, tipo: 'text'|'number'|'date'|'select'|'textarea'|'checkbox', opcoes, obrigatorio,
 *            valor, ajuda, inteiro, passo }]
 * enviar(valores) deve lançar erro para manter o modal aberto.
 */
export function formulario(titulo, campos, enviar, { textoBotao = 'Salvar' } = {}) {
    const entradas = {};
    const form = h('form', { class: 'form', novalidate: true });

    for (const campo of campos) {
        let entrada;
        if (campo.tipo === 'select') {
            entrada = select(campo.opcoes, { valor: campo.valor, classe: '' });
        } else if (campo.tipo === 'textarea') {
            entrada = h('textarea', {}, campo.valor ?? '');
        } else if (campo.tipo === 'checkbox') {
            entrada = h('input', { type: 'checkbox' });
            entrada.checked = Boolean(campo.valor);
        } else {
            entrada = h('input', { type: campo.tipo || 'text', step: campo.passo || (campo.tipo === 'number' ? 'any' : null) });
            if (campo.valor !== undefined && campo.valor !== null) entrada.value = campo.valor;
        }
        entrada.name = campo.nome;
        entrada.id = 'campo-' + campo.nome;
        entradas[campo.nome] = entrada;

        form.append(campo.tipo === 'checkbox'
            ? h('div', { class: 'campo' + (campo.inteiro ? ' inteiro' : '') },
                h('label', { class: 'checkbox', for: entrada.id }, entrada, campo.rotulo),
                campo.ajuda && h('span', { class: 'ajuda' }, campo.ajuda))
            : h('div', { class: 'campo' + (campo.inteiro || campo.tipo === 'textarea' ? ' inteiro' : '') },
                h('label', { for: entrada.id }, campo.rotulo + (campo.obrigatorio ? ' *' : '')),
                entrada,
                campo.ajuda && h('span', { class: 'ajuda' }, campo.ajuda)));
    }

    const salvar = h('button', { type: 'submit', class: 'btn primario' }, textoBotao);
    let fechar;
    form.addEventListener('submit', async e => {
        e.preventDefault();
        const valores = {};
        for (const campo of campos) {
            const entrada = entradas[campo.nome];
            let valor = campo.tipo === 'checkbox' ? entrada.checked : entrada.value.trim();
            if (valor === '' && campo.obrigatorio) {
                toast(`Preencha "${campo.rotulo}".`, 'erro');
                entrada.focus();
                return;
            }
            if (valor === '') valor = null;
            else if (campo.tipo === 'number') valor = Number(valor);
            valores[campo.nome] = valor;
        }
        salvar.disabled = true;
        try {
            await enviar(valores);
            fechar();
        } catch (erro) {
            toast(erro.message, 'erro');
        } finally {
            salvar.disabled = false;
        }
    });

    form.append(h('div', { class: 'rodape inteiro' },
        h('button', { type: 'button', class: 'btn', onclick: () => fechar() }, 'Cancelar'),
        salvar));
    fechar = modal(titulo, form);
    setTimeout(() => form.querySelector('input,select,textarea')?.focus(), 0);
}

export function confirmar(mensagem, aoConfirmar, textoBotao = 'Confirmar') {
    modal('Confirmar', h('p', {}, mensagem), fechar => [
        h('button', { type: 'button', class: 'btn', onclick: fechar }, 'Cancelar'),
        h('button', {
            type: 'button', class: 'btn primario', onclick: async () => {
                try {
                    await aoConfirmar();
                    fechar();
                } catch (erro) {
                    toast(erro.message, 'erro');
                }
            }
        }, textoBotao)
    ]);
}
