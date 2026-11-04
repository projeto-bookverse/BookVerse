package org.example.bookverse.exception;

/**
 * O usuário não tem permissão para a ação (por exemplo, alterar dados de outro usuário).
 * Convertida em resposta 403 pelo {@link TratadorGlobalDeErros}.
 */
public class AcessoNegadoException extends RuntimeException {

	public AcessoNegadoException(String mensagem) {
		super(mensagem);
	}
}
