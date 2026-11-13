package org.example.bookverse.controller;

import org.example.bookverse.config.JwtService;
import org.example.bookverse.entity.EstanteUsuario;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.StatusLeitura;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.repository.EstanteUsuarioRepository;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a estante de ponta a ponta. O dono de cada item é sempre o usuário do token: um ID de usuário
 * enviado na requisição é ignorado. Cada teste roda em uma transação desfeita ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EstanteControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EstanteUsuarioRepository estanteRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private LivroRepository livroRepository;

	@Autowired
	private JwtService jwtService;

	@Test
	void deveAdicionarLivroNaEstanteDoUsuarioDoToken() throws Exception {
		Usuario usuario = novoUsuario();
		Livro livro = novoLivro();

		mockMvc.perform(post("/estantes")
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"livroId":"%s","status":"LENDO","dataInicio":"2026-01-10","dataFim":null}
								""".formatted(livro.getId())))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()))
				.andExpect(jsonPath("$.livroId").value(livro.getId().toString()))
				.andExpect(jsonPath("$.tituloLivro").value("Livro de teste"))
				.andExpect(jsonPath("$.status").value("LENDO"))
				.andExpect(jsonPath("$.dataInicio").value("2026-01-10"));
	}

	@Test
	void deveIgnorarIdDeOutroUsuarioEnviadoNoCorpo() throws Exception {
		// Tentativa de colocar um livro na estante de outra pessoa enviando o ID dela
		Usuario usuario = novoUsuario();
		Usuario vitima = novoUsuario();
		Livro livro = novoLivro();

		mockMvc.perform(post("/estantes")
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","status":"QUERO_LER"}
								""".formatted(vitima.getId(), livro.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()));

		assertTrue(estanteRepository.findByUsuarioId(vitima.getId()).isEmpty());
	}

	@Test
	void naoDeveAdicionarMesmoLivroDuasVezesParaOUsuario() throws Exception {
		Usuario usuario = novoUsuario();
		Livro livro = novoLivro();
		estanteRepository.saveAndFlush(new EstanteUsuario(usuario, livro, StatusLeitura.LENDO));

		mockMvc.perform(post("/estantes")
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"livroId":"%s","status":"LIDO"}
								""".formatted(livro.getId())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Este livro já está na estante do usuário."));
	}

	@Test
	void naoDeveAceitarDataFimAnteriorADataInicio() throws Exception {
		Usuario usuario = novoUsuario();
		Livro livro = novoLivro();

		mockMvc.perform(post("/estantes")
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"livroId":"%s","status":"LIDO","dataInicio":"2026-02-01","dataFim":"2026-01-31"}
								""".formatted(livro.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A data de fim não pode ser anterior à data de início."));
	}

	@Test
	void naoDeveAdicionarLivroInexistente() throws Exception {
		mockMvc.perform(post("/estantes")
						.header(HttpHeaders.AUTHORIZATION, tokenDe(novoUsuario()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"livroId":"%s","status":"QUERO_LER"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Livro não encontrado."));
	}

	@Test
	void naoDeveAcessarEstanteSemToken() throws Exception {
		mockMvc.perform(post("/estantes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"livroId":"%s","status":"QUERO_LER"}
								""".formatted(novoLivro().getId())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deveAtualizarStatusEDatasQuandoSolicitadoPeloDono() throws Exception {
		Usuario usuario = novoUsuario();
		EstanteUsuario item = novoItem(usuario);

		mockMvc.perform(put("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"LIDO","dataInicio":"2026-01-01","dataFim":"2026-02-01"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("LIDO"))
				.andExpect(jsonPath("$.dataInicio").value("2026-01-01"))
				.andExpect(jsonPath("$.dataFim").value("2026-02-01"));
	}

	@Test
	void naoDevePermitirQueOutroUsuarioAltereItemMesmoEnviandoOIdDoDono() throws Exception {
		Usuario dono = novoUsuario();
		EstanteUsuario item = novoItem(dono);
		Usuario outroUsuario = novoUsuario();

		mockMvc.perform(put("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(outroUsuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","status":"ABANDONADO"}
								""".formatted(dono.getId())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Somente o dono do item pode alterá-lo ou removê-lo."));

		assertEquals(StatusLeitura.QUERO_LER, estanteRepository.findById(item.getId()).orElseThrow().getStatus());
	}

	@Test
	void naoDevePermitirQueOutroUsuarioRemovaItemMesmoEnviandoOIdDoDono() throws Exception {
		Usuario dono = novoUsuario();
		EstanteUsuario item = novoItem(dono);

		mockMvc.perform(delete("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(novoUsuario()))
						.param("usuarioId", dono.getId().toString()))
				.andExpect(status().isForbidden());

		assertTrue(estanteRepository.existsById(item.getId()));
	}

	@Test
	void deveRemoverItemQuandoSolicitadoPeloDono() throws Exception {
		Usuario usuario = novoUsuario();
		EstanteUsuario item = novoItem(usuario);

		mockMvc.perform(delete("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario)))
				.andExpect(status().isNoContent());

		assertTrue(estanteRepository.findById(item.getId()).isEmpty());
	}

	@Test
	void naoDeveRemoverItemInexistente() throws Exception {
		mockMvc.perform(delete("/estantes/{id}", UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(novoUsuario())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Item da estante não encontrado."));
	}

	@Test
	void deveListarEstanteDoUsuarioComOTituloDoLivro() throws Exception {
		Usuario usuario = novoUsuario();
		EstanteUsuario item = novoItem(usuario);

		// Qualquer usuário logado pode ver a estante de outro, como um perfil público
		mockMvc.perform(get("/usuarios/{id}/estante", usuario.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(novoUsuario())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(item.getId().toString()))
				.andExpect(jsonPath("$[0].livroId").value(item.getLivro().getId().toString()))
				.andExpect(jsonPath("$[0].tituloLivro").value("Livro de teste"))
				.andExpect(jsonPath("$[0].status").value("QUERO_LER"));
	}

	private Usuario novoUsuario() {
		Usuario usuario = new Usuario("Leitora", "estante-" + UUID.randomUUID() + "@bookverse.com",
				"hash-ficticio", LocalDate.of(2000, 1, 15), TipoConta.LEITOR);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private String tokenDe(Usuario usuario) {
		return "Bearer " + jwtService.gerarToken(usuario).token();
	}

	private Livro novoLivro() {
		return livroRepository.saveAndFlush(new Livro("Livro de teste", "Autor", "Romance"));
	}

	private EstanteUsuario novoItem(Usuario usuario) {
		EstanteUsuario item = new EstanteUsuario(usuario, novoLivro(), StatusLeitura.QUERO_LER);
		return estanteRepository.saveAndFlush(item);
	}
}
