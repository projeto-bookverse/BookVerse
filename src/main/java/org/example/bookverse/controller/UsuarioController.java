package org.example.bookverse.controller;

import org.example.bookverse.dto.UsuarioCadastroRequest;
import org.example.bookverse.dto.UsuarioResponse;
import org.example.bookverse.exception.EmailJaCadastradoException;
import org.example.bookverse.service.UsuarioService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@PostMapping
	public ResponseEntity<?> cadastrar(@RequestBody UsuarioCadastroRequest request) {
		try {
			UsuarioResponse usuario = usuarioService.cadastrar(request);
			return ResponseEntity.created(URI.create("/usuarios/" + usuario.id())).body(usuario);
		} catch (DataIntegrityViolationException exception) {
			if (request != null && request.email() != null
					&& usuarioService.emailCadastrado(request.email())) {
				throw new EmailJaCadastradoException(request.email());
			}
			throw exception;
		}
	}

	@ExceptionHandler(EmailJaCadastradoException.class)
	public ResponseEntity<Map<String, String>> tratarEmailDuplicado(EmailJaCadastradoException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(Map.of("erro", exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> tratarRequisicaoInvalida(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("erro", exception.getMessage()));
	}
}
