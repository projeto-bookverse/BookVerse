package org.example.bookverse.exception;

/**
 * E-mail ou senha incorretos no login. A mensagem não diz qual dos dois está errado,
 * para não revelar quais e-mails estão cadastrados. Convertida em resposta 401 pelo {@link TratadorGlobalDeErros}.
 */
public class CredenciaisInvalidasException extends RuntimeException {

	public CredenciaisInvalidasException() {
		super("E-mail ou senha inválidos.");
	}
}
