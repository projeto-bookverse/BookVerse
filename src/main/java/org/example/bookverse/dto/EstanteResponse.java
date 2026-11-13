package org.example.bookverse.dto;

import org.example.bookverse.entity.EstanteUsuario;
import org.example.bookverse.entity.enums.StatusLeitura;

import java.time.LocalDate;
import java.util.UUID;

/** Item da estante. Traz o título do livro para a tela da estante não precisar buscar cada livro. */
public record EstanteResponse(
		UUID id,
		UUID usuarioId,
		UUID livroId,
		String tituloLivro,
		StatusLeitura status,
		LocalDate dataInicio,
		LocalDate dataFim) {

	public static EstanteResponse from(EstanteUsuario item) {
		return new EstanteResponse(
				item.getId(),
				item.getUsuario().getId(),
				item.getLivro().getId(),
				item.getLivro().getTitulo(),
				item.getStatus(),
				item.getDataInicio(),
				item.getDataFim());
	}
}
