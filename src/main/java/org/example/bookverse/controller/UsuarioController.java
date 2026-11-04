package org.example.bookverse.controller;

import org.example.bookverse.dto.UsuarioCadastroRequest;
import org.example.bookverse.dto.UsuarioResponse;
import org.example.bookverse.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
}
