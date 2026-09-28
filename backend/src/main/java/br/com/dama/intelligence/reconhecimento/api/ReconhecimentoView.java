package br.com.dama.intelligence.reconhecimento.api;

import java.time.LocalDateTime;

public record ReconhecimentoView(Long reconhecimentoId, Long empresaId, Long colaboradorId, Long concedidoPorId,
                                 String tipo, String descricao, LocalDateTime registradoEm) {
}
