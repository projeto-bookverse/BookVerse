package org.example.bookverse.dto;

import org.example.bookverse.entity.Livro;

import java.util.UUID;

public record LivroResponse(
		UUID id,
		String titulo,
		String autor,
		String isbn,
		String capaUrl,
		String sinopse,
		String genero,
		Integer anoPublicacao,
		Integer numPaginas,
		Double mediaNotas,
		long totalAvaliacoes) {

	public static LivroResponse from(Livro livro) {
		return from(livro, 0.0, 0);
	}

	public static LivroResponse from(Livro livro, Double mediaNotas, long totalAvaliacoes) {
		return new LivroResponse(
				livro.getId(),
				livro.getTitulo(),
				livro.getAutor(),
				livro.getIsbn(),
				livro.getCapaUrl(),
				livro.getSinopse(),
				livro.getGenero(),
				livro.getAnoPublicacao(),
				livro.getNumPaginas(),
				arredondarMedia(mediaNotas),
				totalAvaliacoes);
	}

	private static double arredondarMedia(Double mediaNotas) {
		return mediaNotas == null ? 0.0 : Math.round(mediaNotas * 10.0) / 10.0;
	}
}
