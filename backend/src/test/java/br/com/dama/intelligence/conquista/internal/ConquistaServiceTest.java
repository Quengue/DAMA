package br.com.dama.intelligence.conquista.internal;

import br.com.dama.intelligence.auditoria.api.AuditoriaFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorFacade;
import br.com.dama.intelligence.colaborador.api.ColaboradorView;
import br.com.dama.intelligence.conquista.api.ConquistaView;
import br.com.dama.intelligence.conquista.api.CreateConquistaCommand;
import br.com.dama.intelligence.conquista.api.NivelEvolucaoView;
import br.com.dama.intelligence.organizacao.api.OrganizacaoFacade;
import br.com.dama.intelligence.shared.error.ConflictException;
import br.com.dama.intelligence.shared.error.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConquistaServiceTest {

    private static final List<NivelEvolucaoView> NIVEIS = List.of(
            new NivelEvolucaoView(1L, "Iniciante", new BigDecimal("0"), 1, null),
            new NivelEvolucaoView(2L, "Aprendiz", new BigDecimal("100"), 2, null),
            new NivelEvolucaoView(3L, "Praticante", new BigDecimal("300"), 3, null));

    private ConquistaRepository repo;
    private ColaboradorFacade colaboradores;
    private ConquistaService service;

    @BeforeEach
    void setUp() {
        repo = mock(ConquistaRepository.class);
        colaboradores = mock(ColaboradorFacade.class);
        OrganizacaoFacade organizacao = mock(OrganizacaoFacade.class);
        when(organizacao.existeEmpresa(1L)).thenReturn(true);
        service = new ConquistaService(repo, colaboradores, organizacao, mock(AuditoriaFacade.class));
    }

    @Test
    void nivelAtualEProgressoAteOProximo() {
        ConquistaService.Posicao posicao = ConquistaService.posicionar(new BigDecimal("150"), NIVEIS);

        assertThat(posicao.atual().nome()).isEqualTo("Aprendiz");
        assertThat(posicao.proximo().nome()).isEqualTo("Praticante");
        assertThat(posicao.faltam()).isEqualByComparingTo("150");
        assertThat(posicao.percentual()).isEqualTo(25);
    }

    @Test
    void noNivelMaisAltoNaoHaProximoNivel() {
        ConquistaService.Posicao posicao = ConquistaService.posicionar(new BigDecimal("999"), NIVEIS);

        assertThat(posicao.atual().nome()).isEqualTo("Praticante");
        assertThat(posicao.proximo()).isNull();
        assertThat(posicao.percentual()).isEqualTo(100);
    }

    @Test
    void criterioAutomaticoExigeValorMinimo() {
        assertThatThrownBy(() -> service.criar(1L, new CreateConquistaCommand("Cem pontos", null, "PONTOS_TOTAIS", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void conquistaManualNaoAceitaValorMinimo() {
        assertThatThrownBy(() -> service.criar(1L, new CreateConquistaCommand("Destaque", null, "MANUAL", BigDecimal.TEN)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void criterioDesconhecidoEhRecusado() {
        assertThatThrownBy(() -> service.criar(1L, new CreateConquistaCommand("X", null, "LOGINS", BigDecimal.ONE)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("LOGINS");
    }

    @Test
    void conquistaAutomaticaCriadaJaEhAvaliadaParaAEmpresa() {
        when(repo.inserir(1L, "Cem pontos", null, "PONTOS_TOTAIS", new BigDecimal("100"))).thenReturn(7L);
        when(repo.buscar(7L)).thenReturn(Optional.of(conquista(7L, "PONTOS_TOTAIS")));
        when(repo.concederAutomaticas(1L, null, 7L)).thenReturn(List.of());

        service.criar(1L, new CreateConquistaCommand("Cem pontos", null, "PONTOS_TOTAIS", new BigDecimal("100")));

        verify(repo).concederAutomaticas(1L, null, 7L);
    }

    @Test
    void conquistaAutomaticaNaoPodeSerConcedidaManualmente() {
        when(colaboradores.buscarPorId(10L)).thenReturn(colaborador(10L, true));
        when(repo.buscar(7L)).thenReturn(Optional.of(conquista(7L, "PONTOS_TOTAIS")));

        assertThatThrownBy(() -> service.conceder(10L, 7L)).isInstanceOf(ValidationException.class);
        verify(repo, never()).inserirConcessaoManual(any(), any(), any());
    }

    @Test
    void conquistaManualRepetidaEhConflito() {
        when(colaboradores.buscarPorId(10L)).thenReturn(colaborador(10L, true));
        when(repo.buscar(8L)).thenReturn(Optional.of(conquista(8L, "MANUAL")));
        when(repo.possui(10L, 8L)).thenReturn(true);

        assertThatThrownBy(() -> service.conceder(10L, 8L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void colaboradorInativoNaoEhAvaliado() {
        when(colaboradores.buscarPorId(10L)).thenReturn(colaborador(10L, false));

        service.avaliarColaborador(10L);

        verify(repo, never()).concederAutomaticas(any(), any(), any());
    }

    private static ConquistaView conquista(Long id, String criterio) {
        BigDecimal minimo = "MANUAL".equals(criterio) ? null : new BigDecimal("100");
        return new ConquistaView(id, 1L, "Conquista " + id, null, criterio, minimo, true, 0);
    }

    private static ColaboradorView colaborador(Long id, boolean ativo) {
        return new ColaboradorView(id, 1L, 1L, "Operações", "Lucas", "lucas@x.com", "Analista", "Pleno",
                LocalDate.of(2025, 1, 1), ativo);
    }
}
