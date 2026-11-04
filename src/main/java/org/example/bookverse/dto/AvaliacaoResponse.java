package org.example.bookverse.dto;

import org.example.bookverse.entity.Avaliacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record AvaliacaoResponse(
		UUID id,
		UUID usuarioId,
		String nomeUsuario,
		UUID livroId,
		Short nota,
		String textoResenha,
		boolean flagSpoiler,
		LocalDateTime criadoEm) {

	public static AvaliacaoResponse from(Avaliacao avaliacao) {
		return new AvaliacaoResponse(
				avaliacao.getId(),
				avaliacao.getUsuario().getId(),
				avaliacao.getUsuario().getNome(),
				avaliacao.getLivro().getId(),
				avaliacao.getNota(),
				avaliacao.getTextoResenha(),
				avaliacao.isFlagSpoiler(),
				avaliacao.getCriadoEm());
	}
}
