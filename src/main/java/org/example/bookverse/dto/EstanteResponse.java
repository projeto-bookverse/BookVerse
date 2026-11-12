package org.example.bookverse.dto;

import org.example.bookverse.entity.EstanteUsuario;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;
import java.util.UUID;

public record EstanteResponse(
		UUID id,
		UUID usuarioId,
		UUID livroId,
		StatusLeitura status,
		LocalDate dataInicio,
		LocalDate dataFim) {

	public static EstanteResponse from(EstanteUsuario item) {
		return new EstanteResponse(
				item.getId(),
				item.getUsuario().getId(),
				item.getLivro().getId(),
				item.getStatus(),
				item.getDataInicio(),
				item.getDataFim());
	}
}
