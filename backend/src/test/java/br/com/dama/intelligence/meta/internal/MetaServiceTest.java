package br.com.dama.intelligence.meta.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.indicador.api.IndicadorFacade;
import br.com.dama.intelligence.meta.api.MetaView;
import br.com.dama.intelligence.meta.api.RegistrarProgressoCommand;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.shared.error.ValidationException;
import br.com.dama.intelligence.shared.event.DesempenhoAtualizadoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MetaServiceTest {

    private MetaRepository repo;
    private IndicadorFacade indicadores;
    private ApplicationEventPublisher eventos;
    private MetaService service;

    @BeforeEach
    void setUp() {
        repo = mock(MetaRepository.class);
        indicadores = mock(IndicadorFacade.class);
        eventos = mock(ApplicationEventPublisher.class);
        service = new MetaService(repo, mock(ColaboradorFacade.class), mock(OrganizacaoFacade.class), indicadores,
                mock(AuditoriaFacade.class), eventos);
    }

    @Test
    void metaSemIndicadorConcluiQuandoAtingeOuSuperaOAlvo() {
        MetaView meta = meta(null, "Em andamento");
        assertThat(service.atingiuAlvo(meta, new BigDecimal("80"))).isTrue();
        assertThat(service.atingiuAlvo(meta, new BigDecimal("79.9"))).isFalse();
    }

    @Test
    void metaDeIndicadorMenorEhMelhorConcluiQuandoFicaAbaixoDoAlvo() {
        when(indicadores.maiorMelhor(5L)).thenReturn(false);
        MetaView meta = meta(5L, "Em andamento");

        assertThat(service.atingiuAlvo(meta, new BigDecimal("75"))).isTrue();
        assertThat(service.atingiuAlvo(meta, new BigDecimal("90"))).isFalse();
    }

    @Test
    void conclusaoAutomaticaPublicaEventoParaConquistas() {
        when(repo.buscar(1L)).thenReturn(Optional.of(meta(null, "Em andamento")));

        service.registrarProgresso(1L, new RegistrarProgressoCommand(new BigDecimal("100")));

        verify(repo).atualizarStatus(1L, "Concluida");
        verify(eventos).publishEvent(new DesempenhoAtualizadoEvent(10L));
    }

    @Test
    void progressoEmMetaEncerradaEhRecusado() {
        when(repo.buscar(1L)).thenReturn(Optional.of(meta(null, "Cancelada")));

        assertThatThrownBy(() -> service.registrarProgresso(1L, new RegistrarProgressoCommand(BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(eventos);
    }

    private static MetaView meta(Long indicadorId, String status) {
        return new MetaView(1L, 1L, indicadorId, 10L, null, "Meta", new BigDecimal("80"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), status, null);
    }
}
