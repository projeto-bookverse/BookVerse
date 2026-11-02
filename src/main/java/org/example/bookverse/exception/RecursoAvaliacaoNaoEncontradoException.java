package org.example.bookverse.exception;

public class RecursoAvaliacaoNaoEncontradoException extends RuntimeException {

	public RecursoAvaliacaoNaoEncontradoException(String recurso) {
		super(recurso + " não encontrado.");
	}
}
