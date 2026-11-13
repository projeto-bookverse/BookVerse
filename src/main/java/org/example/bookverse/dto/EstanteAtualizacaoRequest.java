package org.example.bookverse.dto;

import jakarta.validation.constraints.NotNull;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;

/** Alteração do status e das datas de um item da estante. Quem altera é o usuário do token. */
public record EstanteAtualizacaoRequest(
		@NotNull(message = "O status de leitura é obrigatório.")
		StatusLeitura status,
		LocalDate dataInicio,
		LocalDate dataFim) {
}
