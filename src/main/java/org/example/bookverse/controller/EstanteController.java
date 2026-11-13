package org.example.bookverse.controller;

import jakarta.validation.Valid;
import org.example.bookverse.config.JwtService;
import org.example.bookverse.dto.EstanteAtualizacaoRequest;
import org.example.bookverse.dto.EstanteRequest;
import org.example.bookverse.dto.EstanteResponse;
import org.example.bookverse.service.EstanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Estante de leitura. Inclusão, alteração e remoção valem sempre para o usuário identificado pelo token. */
@RestController
public class EstanteController {

	private final EstanteService estanteService;

	public EstanteController(EstanteService estanteService) {
		this.estanteService = estanteService;
	}

	@PostMapping("/estantes")
	public ResponseEntity<EstanteResponse> adicionar(
			@Valid @RequestBody EstanteRequest request,
			@AuthenticationPrincipal Jwt token) {
		EstanteResponse item = estanteService.adicionar(request, JwtService.idDoUsuario(token));
		return ResponseEntity.created(URI.create("/estantes/" + item.id())).body(item);
	}

	@PutMapping("/estantes/{id}")
	public EstanteResponse atualizar(
			@PathVariable UUID id,
			@Valid @RequestBody EstanteAtualizacaoRequest request,
			@AuthenticationPrincipal Jwt token) {
		return estanteService.atualizar(id, request, JwtService.idDoUsuario(token));
	}

	@DeleteMapping("/estantes/{id}")
	public ResponseEntity<Void> remover(@PathVariable UUID id, @AuthenticationPrincipal Jwt token) {
		estanteService.remover(id, JwtService.idDoUsuario(token));
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/usuarios/{id}/estante")
	public List<EstanteResponse> listarPorUsuario(@PathVariable UUID id) {
		return estanteService.listarPorUsuario(id);
	}
}
