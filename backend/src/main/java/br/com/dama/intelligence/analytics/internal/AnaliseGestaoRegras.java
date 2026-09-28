package br.com.dama.intelligence.analytics.internal;

import br.com.dama.intelligence.analytics.api.AlertaGestaoView;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Regras das análises de gestão (RF15). Funções puras: recebem linhas já lidas do banco e a data de referência. */
final class AnaliseGestaoRegras {

    static final int DIAS_SEM_ATIVIDADE = 30;

    private static final BigDecimal PIORA_MINIMA = new BigDecimal("0.10");
    private static final BigDecimal PIORA_ALTA = new BigDecimal("0.25");
    private static final double FOLGA_META_EM_RISCO = 0.25;
    private static final BigDecimal FRACAO_MEDIA_EQUIPE = new BigDecimal("0.5");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private AnaliseGestaoRegras() {
    }

    record MetaAberta(Long metaId, String descricao, Long colaboradorId, String colaboradorNome, Long departamentoId,
                      String departamentoNome, BigDecimal valorAlvo, BigDecimal progressoAtual, LocalDate dataInicio,
                      LocalDate dataFim, boolean maiorMelhor) {
        String responsavel() {
            return colaboradorNome != null ? colaboradorNome : "equipe " + departamentoNome;
        }
    }

    record ValorIndicador(Long indicadorId, String indicadorNome, String unidade, boolean maiorMelhor, Long colaboradorId,
                          String colaboradorNome, Long departamentoId, String departamentoNome, LocalDate periodo,
                          BigDecimal valor) {
        String alvo() {
            return colaboradorNome != null ? colaboradorNome : "a equipe " + departamentoNome;
        }
    }

    record Equipe(Long departamentoId, String nome, long colaboradoresAtivos, BigDecimal pontosTotais) {
    }

    record ColaboradorSemAtividade(Long colaboradorId, String nome, Long departamentoId) {
    }

    /** Meta com prazo encerrado (ALTA) ou com progresso bem atrás do tempo decorrido (MEDIA). */
    static Optional<AlertaGestaoView> avaliarMeta(MetaAberta meta, LocalDate hoje) {
        String progresso = meta.progressoAtual() == null ? "sem progresso registrado" : formatar(meta.progressoAtual());
        if (meta.dataFim().isBefore(hoje)) {
            return Optional.of(new AlertaGestaoView("META_VENCIDA", "ALTA",
                    "Meta vencida: " + meta.descricao(),
                    "Prazo terminou em " + DIA.format(meta.dataFim()) + " com " + progresso + " de "
                            + formatar(meta.valorAlvo()) + " (" + meta.responsavel() + ").",
                    "Revisar com o responsável: concluir, replanejar o prazo ou cancelar a meta.",
                    meta.colaboradorId(), meta.departamentoId(), meta.metaId()));
        }
        if (hoje.isBefore(meta.dataInicio()) || !meta.maiorMelhor() || meta.valorAlvo().signum() <= 0) {
            return Optional.empty();
        }
        double duracao = ChronoUnit.DAYS.between(meta.dataInicio(), meta.dataFim()) + 1;
        double decorrido = (ChronoUnit.DAYS.between(meta.dataInicio(), hoje) + 1) / duracao;
        double atingido = meta.progressoAtual() == null ? 0
                : meta.progressoAtual().divide(meta.valorAlvo(), MathContext.DECIMAL64).doubleValue();
        if (decorrido >= 0.5 && atingido < decorrido - FOLGA_META_EM_RISCO) {
            return Optional.of(new AlertaGestaoView("META_EM_RISCO", "MEDIA",
                    "Meta em risco: " + meta.descricao(),
                    Math.round(decorrido * 100) + "% do prazo decorrido e " + Math.round(atingido * 100)
                            + "% do alvo atingido (" + meta.responsavel() + ").",
                    "Acompanhar de perto e remover impedimentos antes de " + DIA.format(meta.dataFim()) + ".",
                    meta.colaboradorId(), meta.departamentoId(), meta.metaId()));
        }
        return Optional.empty();
    }

    /** Compara os dois últimos valores de uma série; piora de 10% ou mais gera alerta (25% ou mais = ALTA). */
    static Optional<AlertaGestaoView> avaliarTendencia(ValorIndicador anterior, ValorIndicador atual) {
        if (anterior.valor().signum() == 0) {
            return Optional.empty();
        }
        BigDecimal variacao = atual.valor().subtract(anterior.valor())
                .divide(anterior.valor().abs(), MathContext.DECIMAL64);
        BigDecimal piora = atual.maiorMelhor() ? variacao.negate() : variacao;
        if (piora.compareTo(PIORA_MINIMA) < 0) {
            return Optional.empty();
        }
        String severidade = piora.compareTo(PIORA_ALTA) >= 0 ? "ALTA" : "MEDIA";
        long percentual = piora.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        String unidade = atual.unidade() == null ? "" : ("%".equals(atual.unidade()) ? "%" : " " + atual.unidade());
        return Optional.of(new AlertaGestaoView("INDICADOR_EM_QUEDA", severidade,
                atual.indicadorNome() + " piorou " + percentual + "% para " + atual.alvo(),
                "De " + formatar(anterior.valor()) + unidade + " em " + MES.format(anterior.periodo()) + " para "
                        + formatar(atual.valor()) + unidade + " em " + MES.format(atual.periodo()) + ".",
                atual.colaboradorId() != null
                        ? "Conversar com o colaborador sobre a causa e avaliar treinamento ou apoio."
                        : "Investigar a causa com a liderança da equipe antes do próximo período.",
                atual.colaboradorId(), atual.departamentoId(), atual.indicadorId()));
    }

    /** Equipes cuja média de pontos por colaborador ativo fica abaixo de metade da média da empresa. */
    static List<AlertaGestaoView> avaliarEquipes(List<Equipe> equipes) {
        List<Equipe> comPessoas = equipes.stream().filter(e -> e.colaboradoresAtivos() > 0).toList();
        long ativos = comPessoas.stream().mapToLong(Equipe::colaboradoresAtivos).sum();
        BigDecimal pontos = comPessoas.stream().map(Equipe::pontosTotais).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<AlertaGestaoView> alertas = new ArrayList<>();
        if (comPessoas.size() < 2 || pontos.signum() <= 0) {
            return alertas;
        }
        BigDecimal mediaEmpresa = pontos.divide(BigDecimal.valueOf(ativos), 2, RoundingMode.HALF_UP);
        for (Equipe equipe : comPessoas) {
            BigDecimal mediaEquipe = equipe.pontosTotais()
                    .divide(BigDecimal.valueOf(equipe.colaboradoresAtivos()), 2, RoundingMode.HALF_UP);
            if (mediaEquipe.compareTo(mediaEmpresa.multiply(FRACAO_MEDIA_EQUIPE)) < 0) {
                alertas.add(new AlertaGestaoView("EQUIPE_ABAIXO_DA_MEDIA", "MEDIA",
                        "Equipe " + equipe.nome() + " abaixo da média de pontos",
                        "Média de " + formatar(mediaEquipe) + " pontos por colaborador ativo, contra "
                                + formatar(mediaEmpresa) + " na empresa.",
                        "Rever metas e desafios da equipe e reconhecer as entregas que estão acontecendo.",
                        null, equipe.departamentoId(), null));
            }
        }
        return alertas;
    }

    static AlertaGestaoView semAtividade(ColaboradorSemAtividade colaborador, LocalDate hoje) {
        return new AlertaGestaoView("SEM_ATIVIDADE_RECENTE", "BAIXA",
                colaborador.nome() + " sem atividade nos últimos " + DIAS_SEM_ATIVIDADE + " dias",
                "Nenhum ponto ou atividade produtiva registrado desde "
                        + DIA.format(hoje.minusDays(DIAS_SEM_ATIVIDADE)) + ".",
                "Verificar engajamento e se há atividades que deveriam estar sendo registradas.",
                colaborador.colaboradorId(), colaborador.departamentoId(), null);
    }

    static int peso(String severidade) {
        return switch (severidade) {
            case "ALTA" -> 0;
            case "MEDIA" -> 1;
            default -> 2;
        };
    }

    /** Número no formato brasileiro, sem zeros à direita (ex.: 74,5). */
    private static String formatar(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
