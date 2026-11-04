package org.example.bookverse.exception;

/**
 * O registro procurado pelo ID informado não existe (usuário, livro, avaliação...).
 * Convertida em resposta 404 pelo {@link TratadorGlobalDeErros}.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}
}
