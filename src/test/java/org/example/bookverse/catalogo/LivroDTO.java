package org.example.bookverse.catalogo;

/**
 * Formato comum de entrada de um livro, independente da origem (arquivo de seed, cadastro manual ou API externa).
 * Toda fonte converte seus dados para este record antes de chamar o {@link LivroImportador}.
 */
public record LivroDTO(
		String titulo,
		String autor,
		String isbn,
		String capaUrl,
		String sinopse,
		String genero,
		Integer anoPublicacao,
		Integer numPaginas) {
}
