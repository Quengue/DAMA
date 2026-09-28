package br.com.dama.intelligence.conquista.api;

import java.util.Arrays;
import java.util.Optional;

/** Como uma conquista é obtida. Todos, exceto MANUAL, são avaliados automaticamente contra valor_minimo. */
public enum CriterioConquista {
    PONTOS_TOTAIS("Pontos acumulados"),
    METAS_CONCLUIDAS("Metas individuais concluídas"),
    DESAFIOS_CONCLUIDOS("Desafios concluídos"),
    RECONHECIMENTOS_RECEBIDOS("Reconhecimentos recebidos"),
    ATIVIDADES_REGISTRADAS("Atividades produtivas registradas"),
    AI_CREDITS_ACUMULADOS("AI Credits acumulados (débitos não descontam)"),
    MANUAL("Concedida manualmente por um gestor");

    private final String descricao;

    CriterioConquista(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }

    public boolean automatico() {
        return this != MANUAL;
    }

    public static Optional<CriterioConquista> de(String codigo) {
        return Arrays.stream(values()).filter(c -> c.name().equals(codigo)).findFirst();
    }
}
