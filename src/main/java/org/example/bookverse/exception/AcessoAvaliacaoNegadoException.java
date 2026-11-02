package org.example.bookverse.exception;

public class AcessoAvaliacaoNegadoException extends RuntimeException {

	public AcessoAvaliacaoNegadoException() {
		super("Somente o autor da avaliação pode alterá-la ou excluí-la.");
	}
}
