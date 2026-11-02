package org.example.bookverse.exception;

public class AvaliacaoDuplicadaException extends RuntimeException {

	public AvaliacaoDuplicadaException() {
		super("Este usuário já avaliou este livro.");
	}
}
