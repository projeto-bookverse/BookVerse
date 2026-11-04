package org.example.bookverse.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converte as exceções de negócio em respostas no formato Problem Details,
 * o mesmo que o Spring usa para os próprios erros (ex.: JSON mal formado).
 * O front-end sempre encontra a mensagem para o usuário no campo "detail".
 * Tem precedência sobre o tratador padrão do Spring para que as mensagens de validação
 * dos DTOs (@NotNull, @Min...) cheguem em português ao usuário.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TratadorGlobalDeErros {

	@ExceptionHandler(DadosInvalidosException.class)
	public ProblemDetail tratarDadosInvalidos(DadosInvalidosException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail tratarValidacao(MethodArgumentNotValidException exception) {
		FieldError erro = exception.getBindingResult().getFieldError();
		String mensagem = erro != null ? erro.getDefaultMessage() : "Dados inválidos.";
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, mensagem);
	}

	@ExceptionHandler(AcessoNegadoException.class)
	public ProblemDetail tratarAcessoNegado(AcessoNegadoException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
	}

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(ConflitoException.class)
	public ProblemDetail tratarConflito(ConflitoException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
	}
}
