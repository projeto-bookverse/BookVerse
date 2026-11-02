package org.example.bookverse.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as exceções de negócio em respostas no formato Problem Details,
 * o mesmo que o Spring usa para os próprios erros (ex.: JSON mal formado).
 * O front-end sempre encontra a mensagem para o usuário no campo "detail".
 */
@RestControllerAdvice
public class TratadorGlobalDeErros {

	@ExceptionHandler(DadosInvalidosException.class)
	public ProblemDetail tratarDadosInvalidos(DadosInvalidosException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(EmailJaCadastradoException.class)
	public ProblemDetail tratarEmailDuplicado(EmailJaCadastradoException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}
}
