package org.example.bookverse.controller;

import org.example.bookverse.config.JwtService;
import org.example.bookverse.dto.UsuarioAtualizacaoRequest;
import org.example.bookverse.dto.UsuarioCadastroRequest;
import org.example.bookverse.dto.UsuarioResponse;
import org.example.bookverse.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@PostMapping
	public ResponseEntity<UsuarioResponse> cadastrar(@RequestBody UsuarioCadastroRequest request) {
		UsuarioResponse usuario = usuarioService.cadastrar(request);
		return ResponseEntity.created(URI.create("/usuarios/" + usuario.id())).body(usuario);
	}

	@GetMapping("/{id}")
	public UsuarioResponse buscarPorId(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
		return usuarioService.buscarPorId(id, JwtService.idDoUsuario(token));
	}

	@PutMapping("/{id}")
	public UsuarioResponse atualizar(
			@PathVariable UUID id,
			@RequestBody UsuarioAtualizacaoRequest request,
			@AuthenticationPrincipal Jwt token) {
		return usuarioService.atualizar(id, request, JwtService.idDoUsuario(token));
	}
}
