package org.example.bookverse.controller;

import jakarta.validation.Valid;
import org.example.bookverse.dto.EstanteAtualizacaoRequest;
import org.example.bookverse.dto.EstanteRequest;
import org.example.bookverse.dto.EstanteResponse;
import org.example.bookverse.service.EstanteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping
public class EstanteController {

	private final EstanteService estanteService;

	public EstanteController(EstanteService estanteService) {
		this.estanteService = estanteService;
	}

	@PostMapping("/estantes")
	public ResponseEntity<EstanteResponse> adicionar(@Valid @RequestBody EstanteRequest request) {
		EstanteResponse item = estanteService.adicionar(request);
		return ResponseEntity.created(URI.create("/estantes/" + item.id())).body(item);
	}

	@PutMapping("/estantes/{id}")
	public EstanteResponse atualizar(
			@PathVariable UUID id,
			@Valid @RequestBody EstanteAtualizacaoRequest request) {
		return estanteService.atualizar(id, request);
	}

	@DeleteMapping("/estantes/{id}")
	public ResponseEntity<Void> remover(
			@PathVariable UUID id,
			@RequestParam UUID usuarioId) {
		estanteService.remover(id, usuarioId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/usuarios/{id}/estante")
	public List<EstanteResponse> listarPorUsuario(@PathVariable UUID id) {
		return estanteService.listarPorUsuario(id);
	}
}
