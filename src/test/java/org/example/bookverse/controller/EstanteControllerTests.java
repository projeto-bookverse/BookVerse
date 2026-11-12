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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	void deveAdicionarLivroNaEstanteERetornarCreated() throws Exception {
		Usuario usuario = novoUsuario();
		Livro livro = novoLivro();

		mockMvc.perform(post("/estantes")
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","livroId":"%s","status":"LENDO",
								 "dataInicio":"2026-01-10","dataFim":null}
								""".formatted(usuario.getId(), livro.getId())))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()))
				.andExpect(jsonPath("$.livroId").value(livro.getId().toString()))
				.andExpect(jsonPath("$.status").value("LENDO"));
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
								{"usuarioId":"%s","livroId":"%s","status":"LIDO"}
								""".formatted(usuario.getId(), livro.getId())))
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
								{"usuarioId":"%s","livroId":"%s","status":"LIDO",
								 "dataInicio":"2026-02-01","dataFim":"2026-01-31"}
								""".formatted(usuario.getId(), livro.getId())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value(
						"A data de fim não pode ser anterior à data de início."));
	}

	@Test
	void deveAtualizarStatusEDatasQuandoSolicitadoPeloDono() throws Exception {
		Usuario usuario = novoUsuario();
		EstanteUsuario item = novoItem(usuario);

		mockMvc.perform(put("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","status":"LIDO",
								 "dataInicio":"2026-01-01","dataFim":"2026-02-01"}
								""".formatted(usuario.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("LIDO"))
				.andExpect(jsonPath("$.dataInicio").value("2026-01-01"))
				.andExpect(jsonPath("$.dataFim").value("2026-02-01"));
	}

	@Test
	void naoDevePermitirAlteracaoOuRemocaoPorOutroUsuario() throws Exception {
		EstanteUsuario item = novoItem(novoUsuario());
		Usuario outroUsuario = novoUsuario();
		UUID outroUsuarioId = outroUsuario.getId();

		mockMvc.perform(put("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(outroUsuario))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId":"%s","status":"LIDO"}
								""".formatted(outroUsuarioId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value(
						"Somente o dono do item pode alterá-lo ou removê-lo."));

		mockMvc.perform(delete("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(outroUsuario))
						.param("usuarioId", outroUsuarioId.toString()))
				.andExpect(status().isForbidden());
	}

	@Test
	void deveRemoverItemQuandoSolicitadoPeloDono() throws Exception {
		Usuario usuario = novoUsuario();
		EstanteUsuario item = novoItem(usuario);

		mockMvc.perform(delete("/estantes/{id}", item.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario))
						.param("usuarioId", usuario.getId().toString()))
				.andExpect(status().isNoContent());
	}

	@Test
	void deveListarEstanteDoUsuario() throws Exception {
		Usuario usuario = novoUsuario();
		EstanteUsuario item = novoItem(usuario);

		mockMvc.perform(get("/usuarios/{id}/estante", usuario.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDe(usuario)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(item.getId().toString()))
				.andExpect(jsonPath("$[0].livroId").value(item.getLivro().getId().toString()));
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
