package org.example.bookverse.dto;

import jakarta.validation.constraints.NotNull;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;
import java.util.UUID;

public record EstanteRequest(
		@NotNull(message = "O identificador do usuário é obrigatório.")
		UUID usuarioId,
		@NotNull(message = "O identificador do livro é obrigatório.")
		UUID livroId,
		@NotNull(message = "O status de leitura é obrigatório.")
		StatusLeitura status,
		LocalDate dataInicio,
		LocalDate dataFim) {
}
