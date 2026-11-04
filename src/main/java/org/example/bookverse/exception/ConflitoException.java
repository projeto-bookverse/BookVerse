package org.example.bookverse.exception;

/**
 * Dado que deveria ser único já existe (e-mail, ISBN, avaliação ou item de estante repetido).
 * Convertida em resposta 409 pelo {@link TratadorGlobalDeErros}.
 */
public class ConflitoException extends RuntimeException {

	public ConflitoException(String mensagem) {
		super(mensagem);
	}
}
