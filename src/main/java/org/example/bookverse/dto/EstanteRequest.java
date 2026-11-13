package org.example.bookverse.dto;

import jakarta.validation.constraints.NotNull;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;
import java.util.UUID;

/** Inclusão de um livro na estante. O dono do item é sempre o usuário do token, nunca um ID enviado aqui. */
public record EstanteRequest(
		@NotNull(message = "O identificador do livro é obrigatório.")
		UUID livroId,
		@NotNull(message = "O status de leitura é obrigatório.")
		StatusLeitura status,
		LocalDate dataInicio,
		LocalDate dataFim) {
}
