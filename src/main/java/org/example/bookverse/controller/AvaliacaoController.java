package org.example.bookverse.controller;

import jakarta.validation.Valid;
import org.example.bookverse.config.JwtService;
import org.example.bookverse.dto.AvaliacaoAtualizacaoRequest;
import org.example.bookverse.dto.AvaliacaoCadastroRequest;
import org.example.bookverse.dto.AvaliacaoResponse;
import org.example.bookverse.service.AvaliacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class AvaliacaoController {

	private final AvaliacaoService avaliacaoService;

	public AvaliacaoController(AvaliacaoService avaliacaoService) {
		this.avaliacaoService = avaliacaoService;
	}

	@PostMapping("/avaliacoes")
	public ResponseEntity<AvaliacaoResponse> cadastrar(
			@Valid @RequestBody AvaliacaoCadastroRequest request,
			@AuthenticationPrincipal Jwt token) {
		AvaliacaoResponse avaliacao = avaliacaoService.cadastrar(request, JwtService.idDoUsuario(token));
		return ResponseEntity.created(URI.create("/avaliacoes/" + avaliacao.id())).body(avaliacao);
	}

	@GetMapping("/livros/{id}/avaliacoes")
	public List<AvaliacaoResponse> listarPorLivro(@PathVariable UUID id) {
		return avaliacaoService.listarPorLivro(id);
	}

	@GetMapping("/usuarios/{id}/avaliacoes")
	public List<AvaliacaoResponse> listarPorUsuario(@PathVariable UUID id) {
		return avaliacaoService.listarPorUsuario(id);
	}

	@GetMapping("/avaliacoes/{id}")
	public AvaliacaoResponse buscarPorId(@PathVariable UUID id) {
		return avaliacaoService.buscarPorId(id);
	}

	@PutMapping("/avaliacoes/{id}")
	public AvaliacaoResponse atualizar(
			@PathVariable UUID id,
			@Valid @RequestBody AvaliacaoAtualizacaoRequest request,
			@AuthenticationPrincipal Jwt token) {
		return avaliacaoService.atualizar(id, request, JwtService.idDoUsuario(token));
	}

	@DeleteMapping("/avaliacoes/{id}")
	public ResponseEntity<Void> excluir(
			@PathVariable UUID id,
			@AuthenticationPrincipal Jwt token) {
		avaliacaoService.excluir(id, JwtService.idDoUsuario(token));
		return ResponseEntity.noContent().build();
	}
}
