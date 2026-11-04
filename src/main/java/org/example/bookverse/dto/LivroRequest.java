package org.example.bookverse.dto;

public record LivroRequest(
		String titulo,
		String autor,
		String isbn,
		String capaUrl,
		String sinopse,
		String genero,
		Integer anoPublicacao,
		Integer numPaginas) {
}
