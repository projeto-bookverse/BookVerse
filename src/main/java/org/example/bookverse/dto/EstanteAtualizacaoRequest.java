package org.example.bookverse.dto;

import jakarta.validation.constraints.NotNull;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;
import java.util.UUID;

public record EstanteAtualizacaoRequest(
		@NotNull(message = "O identificador do usuário é obrigatório para validar a titularidade.")
		UUID usuarioId,
		@NotNull(message = "O status de leitura é obrigatório.")
		StatusLeitura status,
		LocalDate dataInicio,
		LocalDate dataFim) {
}
