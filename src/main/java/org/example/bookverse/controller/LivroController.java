package org.example.bookverse.controller;

import org.example.bookverse.dto.LivroRequest;
import org.example.bookverse.dto.LivroResponse;
import org.example.bookverse.dto.PaginaResponse;
import org.example.bookverse.service.LivroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/livros")
public class LivroController {

	private final LivroService livroService;

	public LivroController(LivroService livroService) {
		this.livroService = livroService;
	}

	/** Catálogo paginado: page começa em 0 e size tem padrão de 20 livros. */
	@GetMapping
	public PaginaResponse<LivroResponse> listar(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + LivroService.LIVROS_POR_PAGINA) int size) {
		return livroService.listar(page, size);
	}

	@GetMapping("/{id}")
	public LivroResponse buscarPorId(@PathVariable UUID id) {
		return livroService.buscarPorId(id);
	}

	@PostMapping
	public ResponseEntity<LivroResponse> cadastrar(@RequestBody LivroRequest request) {
		LivroResponse livro = livroService.cadastrar(request);
		return ResponseEntity.created(URI.create("/livros/" + livro.id())).body(livro);
	}

	@PutMapping("/{id}")
	public LivroResponse atualizar(@PathVariable UUID id, @RequestBody LivroRequest request) {
		return livroService.atualizar(id, request);
	}
}
