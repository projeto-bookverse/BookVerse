package org.example.bookverse.controller;

import org.example.bookverse.dto.LoginRequest;
import org.example.bookverse.dto.TokenResponse;
import org.example.bookverse.service.AutenticacaoService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/autenticacao")
public class AutenticacaoController {

	private final AutenticacaoService autenticacaoService;

	public AutenticacaoController(AutenticacaoService autenticacaoService) {
		this.autenticacaoService = autenticacaoService;
	}

	@PostMapping("/login")
	public TokenResponse entrar(@RequestBody LoginRequest request) {
		return autenticacaoService.entrar(request);
	}
}
