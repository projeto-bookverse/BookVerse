package org.example.bookverse.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AvaliacaoExceptionHandler {

	@ExceptionHandler(AvaliacaoDuplicadaException.class)
	public ResponseEntity<ProblemDetail> tratarAvaliacaoDuplicada(AvaliacaoDuplicadaException exception) {
		return criarResposta(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(RecursoAvaliacaoNaoEncontradoException.class)
	public ResponseEntity<ProblemDetail> tratarRecursoNaoEncontrado(
			RecursoAvaliacaoNaoEncontradoException exception) {
		return criarResposta(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(AcessoAvaliacaoNegadoException.class)
	public ResponseEntity<ProblemDetail> tratarAcessoNegado(AcessoAvaliacaoNegadoException exception) {
		return criarResposta(HttpStatus.FORBIDDEN, exception.getMessage());
	}

	private ResponseEntity<ProblemDetail> criarResposta(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}
}
