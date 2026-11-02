package org.example.bookverse.controller;

import org.example.bookverse.dto.AvaliacaoAtualizacaoRequest;
import org.example.bookverse.dto.AvaliacaoCadastroRequest;
import org.example.bookverse.dto.AvaliacaoResponse;
import org.example.bookverse.exception.AcessoAvaliacaoNegadoException;
import org.example.bookverse.exception.AvaliacaoDuplicadaException;
import org.example.bookverse.exception.RecursoAvaliacaoNaoEncontradoException;
import org.example.bookverse.service.AvaliacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvaliacaoController.class)
class AvaliacaoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AvaliacaoService avaliacaoService;

	@Test
	void deveCadastrarAvaliacaoERetornarCreated() throws Exception {
		UUID id = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		UUID livroId = UUID.randomUUID();
		when(avaliacaoService.cadastrar(any(AvaliacaoCadastroRequest.class)))
				.thenReturn(response(id, usuarioId, livroId));

		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","nota":5,"textoResenha":"Excelente","flagSpoiler":false}
								""".formatted(usuarioId, livroId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/avaliacoes/" + id))
				.andExpect(jsonPath("$.nota").value(5))
				.andExpect(jsonPath("$.flagSpoiler").value(false));
	}

	@Test
	void deveRejeitarNotaForaDoIntervaloComProblemDetails() throws Exception {
		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","nota":6,"textoResenha":"Resenha","flagSpoiler":false}
								""".formatted(UUID.randomUUID(), UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void deveRejeitarResenhaVaziaComProblemDetails() throws Exception {
		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","nota":4,"textoResenha":"  ","flagSpoiler":false}
								""".formatted(UUID.randomUUID(), UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void deveRejeitarResenhaAcimaDoLimiteComProblemDetails() throws Exception {
		String textoLongo = "R".repeat(5001);
		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","nota":4,"textoResenha":"%s","flagSpoiler":false}
								""".formatted(UUID.randomUUID(), UUID.randomUUID(), textoLongo)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void deveRetornarConflitoParaAvaliacaoDuplicada() throws Exception {
		doThrow(new AvaliacaoDuplicadaException())
				.when(avaliacaoService).cadastrar(any(AvaliacaoCadastroRequest.class));

		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","nota":4,"textoResenha":"Resenha","flagSpoiler":false}
								""".formatted(UUID.randomUUID(), UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Este usuário já avaliou este livro."))
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void deveRetornarNotFoundParaUsuarioOuLivroInexistente() throws Exception {
		doThrow(new RecursoAvaliacaoNaoEncontradoException("Livro"))
				.when(avaliacaoService).cadastrar(any(AvaliacaoCadastroRequest.class));

		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","nota":4,"textoResenha":"Resenha","flagSpoiler":false}
								""".formatted(UUID.randomUUID(), UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Livro não encontrado."))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void deveConsultarAvaliacaoPorId() throws Exception {
		UUID id = UUID.randomUUID();
		when(avaliacaoService.buscarPorId(id)).thenReturn(response(id, UUID.randomUUID(), UUID.randomUUID()));

		mockMvc.perform(get("/avaliacoes/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void deveConsultarAvaliacoesPorLivro() throws Exception {
		UUID livroId = UUID.randomUUID();
		when(avaliacaoService.listarPorLivro(livroId)).thenReturn(List.of(
				response(UUID.randomUUID(), UUID.randomUUID(), livroId)));

		mockMvc.perform(get("/livros/{id}/avaliacoes", livroId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].livroId").value(livroId.toString()));
	}

	@Test
	void deveConsultarAvaliacoesPorUsuario() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		when(avaliacaoService.listarPorUsuario(usuarioId)).thenReturn(List.of(
				response(UUID.randomUUID(), usuarioId, UUID.randomUUID())));

		mockMvc.perform(get("/usuarios/{id}/avaliacoes", usuarioId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].usuarioId").value(usuarioId.toString()));
	}

	@Test
	void deveAtualizarAvaliacao() throws Exception {
		UUID id = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		when(avaliacaoService.atualizar(eq(id), any(AvaliacaoAtualizacaoRequest.class)))
				.thenReturn(response(id, usuarioId, UUID.randomUUID()));

		mockMvc.perform(put("/avaliacoes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","nota":3,"textoResenha":"Atualizada","flagSpoiler":true}
								""".formatted(usuarioId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void deveImpedirAtualizacaoPorOutroUsuario() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new AcessoAvaliacaoNegadoException())
				.when(avaliacaoService).atualizar(eq(id), any(AvaliacaoAtualizacaoRequest.class));

		mockMvc.perform(put("/avaliacoes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","nota":3,"textoResenha":"Alterada","flagSpoiler":false}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void deveExcluirAvaliacao() throws Exception {
		UUID id = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();

		mockMvc.perform(delete("/avaliacoes/{id}", id).param("usuarioId", usuarioId.toString()))
				.andExpect(status().isNoContent());

		verify(avaliacaoService).excluir(id, usuarioId);
	}

	@Test
	void deveImpedirExclusaoPorOutroUsuario() throws Exception {
		UUID id = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		doThrow(new AcessoAvaliacaoNegadoException()).when(avaliacaoService).excluir(id, usuarioId);

		mockMvc.perform(delete("/avaliacoes/{id}", id).param("usuarioId", usuarioId.toString()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").isNotEmpty())
				.andExpect(jsonPath("$.status").value(403));
	}

	private AvaliacaoResponse response(UUID id, UUID usuarioId, UUID livroId) {
		return new AvaliacaoResponse(id, usuarioId, livroId, (short) 5, "Resenha", false,
				LocalDateTime.now(), LocalDateTime.now());
	}
}
