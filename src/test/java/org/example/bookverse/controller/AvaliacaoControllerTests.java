package org.example.bookverse.controller;

import org.example.bookverse.config.SegurancaConfig;
import org.example.bookverse.dto.AvaliacaoAtualizacaoRequest;
import org.example.bookverse.dto.AvaliacaoCadastroRequest;
import org.example.bookverse.dto.AvaliacaoResponse;
import org.example.bookverse.exception.AcessoNegadoException;
import org.example.bookverse.exception.ConflitoException;
import org.example.bookverse.exception.RecursoNaoEncontradoException;
import org.example.bookverse.service.AvaliacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o controller de avaliações com o service simulado e as regras de segurança reais (SegurancaConfig).
 * As requisições levam um token de leitor; o ID do usuário vem do token, nunca do corpo da requisição.
 */
@WebMvcTest(AvaliacaoController.class)
@Import(SegurancaConfig.class)
class AvaliacaoControllerTests {

	private static final UUID USUARIO_LOGADO = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AvaliacaoService avaliacaoService;

	@Test
	void deveCadastrarAvaliacaoComOUsuarioDoToken() throws Exception {
		UUID id = UUID.randomUUID();
		UUID livroId = UUID.randomUUID();
		when(avaliacaoService.cadastrar(any(AvaliacaoCadastroRequest.class), eq(USUARIO_LOGADO)))
				.thenReturn(response(id, USUARIO_LOGADO, livroId));

		mockMvc.perform(post("/avaliacoes").with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"livroId":"%s","nota":5,"textoResenha":"Excelente","flagSpoiler":false}
								""".formatted(livroId)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/avaliacoes/" + id))
				.andExpect(jsonPath("$.nota").value(5))
				.andExpect(jsonPath("$.usuarioId").value(USUARIO_LOGADO.toString()));

		verify(avaliacaoService).cadastrar(any(AvaliacaoCadastroRequest.class), eq(USUARIO_LOGADO));
	}

	@Test
	void naoDeveCadastrarSemToken() throws Exception {
		mockMvc.perform(post("/avaliacoes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(cadastro(4, "Resenha")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("É necessário entrar no sistema para acessar este recurso."));
	}

	@Test
	void deveRejeitarNotaForaDoIntervaloComProblemDetails() throws Exception {
		mockMvc.perform(post("/avaliacoes").with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(cadastro(6, "Resenha")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A nota deve ser entre 1 e 5."))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void deveAceitarAvaliacaoSemResenha() throws Exception {
		// A resenha é opcional (RN19): o usuário pode avaliar só com as estrelas.
		when(avaliacaoService.cadastrar(any(AvaliacaoCadastroRequest.class), eq(USUARIO_LOGADO)))
				.thenReturn(response(UUID.randomUUID(), USUARIO_LOGADO, UUID.randomUUID()));

		mockMvc.perform(post("/avaliacoes").with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(cadastro(4, null)))
				.andExpect(status().isCreated());
	}

	@Test
	void deveRejeitarResenhaAcimaDoLimiteComProblemDetails() throws Exception {
		mockMvc.perform(post("/avaliacoes").with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(cadastro(4, "R".repeat(5001))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A resenha deve ter no máximo 5000 caracteres."));
	}

	@Test
	void deveRetornarConflitoParaAvaliacaoDuplicada() throws Exception {
		doThrow(new ConflitoException("Este usuário já avaliou este livro."))
				.when(avaliacaoService).cadastrar(any(AvaliacaoCadastroRequest.class), eq(USUARIO_LOGADO));

		mockMvc.perform(post("/avaliacoes").with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(cadastro(4, "Resenha")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Este usuário já avaliou este livro."))
				.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void deveRetornarNotFoundParaLivroInexistente() throws Exception {
		doThrow(new RecursoNaoEncontradoException("Livro não encontrado."))
				.when(avaliacaoService).cadastrar(any(AvaliacaoCadastroRequest.class), eq(USUARIO_LOGADO));

		mockMvc.perform(post("/avaliacoes").with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content(cadastro(4, "Resenha")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Livro não encontrado."))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void deveConsultarAvaliacaoPorId() throws Exception {
		UUID id = UUID.randomUUID();
		when(avaliacaoService.buscarPorId(id)).thenReturn(response(id, UUID.randomUUID(), UUID.randomUUID()));

		mockMvc.perform(get("/avaliacoes/{id}", id).with(leitorLogado()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void deveConsultarAvaliacoesPorLivro() throws Exception {
		UUID livroId = UUID.randomUUID();
		when(avaliacaoService.listarPorLivro(livroId)).thenReturn(List.of(
				response(UUID.randomUUID(), UUID.randomUUID(), livroId)));

		mockMvc.perform(get("/livros/{id}/avaliacoes", livroId).with(leitorLogado()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].livroId").value(livroId.toString()));
	}

	@Test
	void deveConsultarAvaliacoesPorUsuario() throws Exception {
		UUID usuarioId = UUID.randomUUID();
		when(avaliacaoService.listarPorUsuario(usuarioId)).thenReturn(List.of(
				response(UUID.randomUUID(), usuarioId, UUID.randomUUID())));

		mockMvc.perform(get("/usuarios/{id}/avaliacoes", usuarioId).with(leitorLogado()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].usuarioId").value(usuarioId.toString()));
	}

	@Test
	void deveAtualizarAvaliacao() throws Exception {
		UUID id = UUID.randomUUID();
		when(avaliacaoService.atualizar(eq(id), any(AvaliacaoAtualizacaoRequest.class), eq(USUARIO_LOGADO)))
				.thenReturn(response(id, USUARIO_LOGADO, UUID.randomUUID()));

		mockMvc.perform(put("/avaliacoes/{id}", id).with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nota":3,"textoResenha":"Atualizada","flagSpoiler":true}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()));
	}

	@Test
	void deveImpedirAtualizacaoPorOutroUsuario() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new AcessoNegadoException("Somente o autor da avaliação pode alterá-la ou excluí-la."))
				.when(avaliacaoService).atualizar(eq(id), any(AvaliacaoAtualizacaoRequest.class), eq(USUARIO_LOGADO));

		mockMvc.perform(put("/avaliacoes/{id}", id).with(leitorLogado())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nota":3,"textoResenha":"Alterada","flagSpoiler":false}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Somente o autor da avaliação pode alterá-la ou excluí-la."))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void deveExcluirAvaliacao() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete("/avaliacoes/{id}", id).with(leitorLogado()))
				.andExpect(status().isNoContent());

		verify(avaliacaoService).excluir(id, USUARIO_LOGADO);
	}

	@Test
	void deveImpedirExclusaoPorOutroUsuario() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new AcessoNegadoException("Somente o autor da avaliação pode alterá-la ou excluí-la."))
				.when(avaliacaoService).excluir(id, USUARIO_LOGADO);

		mockMvc.perform(delete("/avaliacoes/{id}", id).with(leitorLogado()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403));
	}

	/** Simula um token válido de leitor, com o ID do usuário no campo "subject". */
	private static RequestPostProcessor leitorLogado() {
		return jwt().jwt(token -> token
				.subject(USUARIO_LOGADO.toString())
				.claim(SegurancaConfig.CLAIM_TIPO_CONTA, "LEITOR"));
	}

	private static String cadastro(int nota, String resenha) {
		String resenhaJson = resenha == null ? "null" : "\"" + resenha + "\"";
		return """
				{"livroId":"%s","nota":%d,"textoResenha":%s,"flagSpoiler":false}
				""".formatted(UUID.randomUUID(), nota, resenhaJson);
	}

	private AvaliacaoResponse response(UUID id, UUID usuarioId, UUID livroId) {
		return new AvaliacaoResponse(id, usuarioId, "Leitora", livroId, (short) 5, "Resenha", false,
				LocalDateTime.now());
	}
}
