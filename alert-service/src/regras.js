'use strict';

/**
 * Regras dos alertas de gestão (RF15) executadas pelo alert-service.
 * São funções puras: recebem os dados já lidos do banco pelo backend e a data de referência.
 * Espelham as regras de AnaliseGestaoRegras.java (usadas como fallback local no backend).
 */

const DIAS_SEM_ATIVIDADE = 30;
const PIORA_MINIMA = 0.1;
const PIORA_ALTA = 0.25;
const FOLGA_META_EM_RISCO = 0.25;
const FRACAO_MEDIA_EQUIPE = 0.5;
const EPSILON = 1e-9;
const MS_DIA = 24 * 60 * 60 * 1000;

// ---------- datas (AAAA-MM-DD, sempre em UTC para não depender do fuso do servidor) ----------

function paraUtc(texto) {
  const [ano, mes, dia] = String(texto).slice(0, 10).split('-').map(Number);
  return Date.UTC(ano, mes - 1, dia);
}

function diasEntre(inicio, fim) {
  return Math.round((paraUtc(fim) - paraUtc(inicio)) / MS_DIA);
}

function formatarDia(textoOuMs) {
  const d = new Date(typeof textoOuMs === 'number' ? textoOuMs : paraUtc(textoOuMs));
  const dd = String(d.getUTCDate()).padStart(2, '0');
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getUTCFullYear()}`;
}

function formatarMes(texto) {
  const d = new Date(paraUtc(texto));
  return `${String(d.getUTCMonth() + 1).padStart(2, '0')}/${d.getUTCFullYear()}`;
}

// ---------- números ----------

/** Número no formato brasileiro, sem zeros à direita (ex.: 74,5). */
function formatar(valor) {
  return String(Number(valor)).replace('.', ',');
}

function arredondar2(valor) {
  return Math.round((valor + Number.EPSILON) * 100) / 100;
}

function alerta(tipo, severidade, titulo, detalhe, recomendacao, colaboradorId, departamentoId, referenciaId) {
  return {
    tipo,
    severidade,
    titulo,
    detalhe,
    recomendacao,
    colaboradorId: colaboradorId ?? null,
    departamentoId: departamentoId ?? null,
    referenciaId: referenciaId ?? null,
  };
}

// ---------- regras ----------

/** Meta com prazo encerrado (ALTA) ou com progresso bem atrás do tempo decorrido (MEDIA). */
function avaliarMeta(meta, hoje) {
  const responsavel = meta.colaboradorNome != null ? meta.colaboradorNome : `equipe ${meta.departamentoNome}`;
  const progresso = meta.progressoAtual == null ? 'sem progresso registrado' : formatar(meta.progressoAtual);

  if (paraUtc(meta.dataFim) < paraUtc(hoje)) {
    return alerta(
      'META_VENCIDA',
      'ALTA',
      `Meta vencida: ${meta.descricao}`,
      `Prazo terminou em ${formatarDia(meta.dataFim)} com ${progresso} de ${formatar(meta.valorAlvo)} (${responsavel}).`,
      'Revisar com o responsável: concluir, replanejar o prazo ou cancelar a meta.',
      meta.colaboradorId, meta.departamentoId, meta.metaId);
  }
  if (paraUtc(hoje) < paraUtc(meta.dataInicio) || !meta.maiorMelhor || Number(meta.valorAlvo) <= 0) {
    return null;
  }
  const duracao = diasEntre(meta.dataInicio, meta.dataFim) + 1;
  const decorrido = (diasEntre(meta.dataInicio, hoje) + 1) / duracao;
  const atingido = meta.progressoAtual == null ? 0 : Number(meta.progressoAtual) / Number(meta.valorAlvo);
  if (decorrido >= 0.5 && atingido < decorrido - FOLGA_META_EM_RISCO) {
    return alerta(
      'META_EM_RISCO',
      'MEDIA',
      `Meta em risco: ${meta.descricao}`,
      `${Math.round(decorrido * 100)}% do prazo decorrido e ${Math.round(atingido * 100)}% do alvo atingido (${responsavel}).`,
      `Acompanhar de perto e remover impedimentos antes de ${formatarDia(meta.dataFim)}.`,
      meta.colaboradorId, meta.departamentoId, meta.metaId);
  }
  return null;
}

/** Compara os dois últimos valores de uma série; piora de 10% ou mais gera alerta (25% ou mais = ALTA). */
function avaliarTendencia(anterior, atual) {
  const valorAnterior = Number(anterior.valor);
  const valorAtual = Number(atual.valor);
  if (valorAnterior === 0) {
    return null;
  }
  const variacao = (valorAtual - valorAnterior) / Math.abs(valorAnterior);
  const piora = atual.maiorMelhor ? -variacao : variacao;
  if (piora < PIORA_MINIMA - EPSILON) {
    return null;
  }
  const severidade = piora >= PIORA_ALTA - EPSILON ? 'ALTA' : 'MEDIA';
  const percentual = Math.round(piora * 100);
  const unidade = atual.unidade == null ? '' : (atual.unidade === '%' ? '%' : ` ${atual.unidade}`);
  const alvo = atual.colaboradorNome != null ? atual.colaboradorNome : `a equipe ${atual.departamentoNome}`;
  return alerta(
    'INDICADOR_EM_QUEDA',
    severidade,
    `${atual.indicadorNome} piorou ${percentual}% para ${alvo}`,
    `De ${formatar(valorAnterior)}${unidade} em ${formatarMes(anterior.periodo)} para ${formatar(valorAtual)}${unidade} em ${formatarMes(atual.periodo)}.`,
    atual.colaboradorId != null
      ? 'Conversar com o colaborador sobre a causa e avaliar treinamento ou apoio.'
      : 'Investigar a causa com a liderança da equipe antes do próximo período.',
    atual.colaboradorId, atual.departamentoId, atual.indicadorId);
}

/** Equipes cuja média de pontos por colaborador ativo fica abaixo de metade da média da empresa. */
function avaliarEquipes(equipes) {
  const comPessoas = equipes.filter((e) => Number(e.colaboradoresAtivos) > 0);
  const ativos = comPessoas.reduce((soma, e) => soma + Number(e.colaboradoresAtivos), 0);
  const pontos = comPessoas.reduce((soma, e) => soma + Number(e.pontosTotais), 0);
  if (comPessoas.length < 2 || pontos <= 0) {
    return [];
  }
  const mediaEmpresa = arredondar2(pontos / ativos);
  const alertas = [];
  for (const equipe of comPessoas) {
    const mediaEquipe = arredondar2(Number(equipe.pontosTotais) / Number(equipe.colaboradoresAtivos));
    if (mediaEquipe < mediaEmpresa * FRACAO_MEDIA_EQUIPE) {
      alertas.push(alerta(
        'EQUIPE_ABAIXO_DA_MEDIA',
        'MEDIA',
        `Equipe ${equipe.nome} abaixo da média de pontos`,
        `Média de ${formatar(mediaEquipe)} pontos por colaborador ativo, contra ${formatar(mediaEmpresa)} na empresa.`,
        'Rever metas e desafios da equipe e reconhecer as entregas que estão acontecendo.',
        null, equipe.departamentoId, null));
    }
  }
  return alertas;
}

function semAtividade(colaborador, hoje) {
  return alerta(
    'SEM_ATIVIDADE_RECENTE',
    'BAIXA',
    `${colaborador.nome} sem atividade nos últimos ${DIAS_SEM_ATIVIDADE} dias`,
    `Nenhum ponto ou atividade produtiva registrado desde ${formatarDia(paraUtc(hoje) - DIAS_SEM_ATIVIDADE * MS_DIA)}.`,
    'Verificar engajamento e se há atividades que deveriam estar sendo registradas.',
    colaborador.colaboradorId, colaborador.departamentoId, null);
}

/**
 * Aplica todas as regras. A ordem de saída é a mesma do backend (metas, tendências, equipes, sem atividade);
 * a ordenação por severidade e a contagem continuam sendo feitas pelo backend.
 */
function avaliar(entrada) {
  const hoje = entrada.dataReferencia;
  const alertas = [];
  for (const meta of entrada.metas) {
    const a = avaliarMeta(meta, hoje);
    if (a) alertas.push(a);
  }
  for (const par of entrada.tendencias) {
    const a = avaliarTendencia(par.anterior, par.atual);
    if (a) alertas.push(a);
  }
  alertas.push(...avaliarEquipes(entrada.equipes));
  for (const colaborador of entrada.colaboradoresSemAtividade) {
    alertas.push(semAtividade(colaborador, hoje));
  }
  return alertas;
}

module.exports = {
  DIAS_SEM_ATIVIDADE,
  avaliar,
  avaliarMeta,
  avaliarTendencia,
  avaliarEquipes,
  semAtividade,
};
