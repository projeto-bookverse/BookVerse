package org.example.bookverse.controller;

import org.example.bookverse.config.JwtService;
import org.example.bookverse.entity.Avaliacao;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.repository.AvaliacaoRepository;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o cadastro e a edição de livros de ponta a ponta (HTTP com token → Service → PostgreSQL local).
 * Só administradores cadastram livros, então as requisições usam o token de um admin.
 * Cada teste roda em uma transação desfeita ao final, então nada fica salvo no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LivroControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private LivroRepository livroRepository;

	@Autowired
	private AvaliacaoRepository avaliacaoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private JwtService jwtService;

	private String tokenDeAdmin;

	private String tokenDeLeitor;

	@BeforeEach
	void configurar() {
		tokenDeAdmin = tokenDe(novoUsuario(TipoConta.ADMIN));
		tokenDeLeitor = tokenDe(novoUsuario(TipoConta.LEITOR));
	}

	@Test
	void deveListarLivrosComVinteItensPorPagina() throws Exception {
		long existentes = livroRepository.count();
		for (int indice = 0; indice < 21; indice++) {
			livroRepository.save(new Livro("Livro " + indice, "Autor", "Romance"));
		}
		livroRepository.flush();
		long total = existentes + 21;

		mockMvc.perform(get("/livros")
						.header(HttpHeaders.AUTHORIZATION, tokenDeLeitor)
						.param("page", "0"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(20))
				.andExpect(jsonPath("$.totalElements").value(total))
				.andExpect(jsonPath("$.number").value(0));

		mockMvc.perform(get("/livros")
						.header(HttpHeaders.AUTHORIZATION, tokenDeLeitor)
						.param("page", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(Math.min(20, total - 20)))
				.andExpect(jsonPath("$.number").value(1));
	}

	@Test
	void deveRetornarMediaArredondadaEQuantidadeDeAvaliacoes() throws Exception {
		Livro livro = livroRepository.saveAndFlush(new Livro("Vidas Secas", "Graciliano Ramos", "Romance"));
		short[] notas = {4, 4, 5};
		for (short nota : notas) {
			Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario(TipoConta.LEITOR));
			avaliacaoRepository.saveAndFlush(new Avaliacao(usuario, livro, nota));
		}

		mockMvc.perform(get("/livros/{id}", livro.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDeLeitor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mediaNotas").value(4.3))
				.andExpect(jsonPath("$.totalAvaliacoes").value(3));
	}

	@Test
	void deveRetornarZeroQuandoLivroAindaNaoTemAvaliacoes() throws Exception {
		Livro livro = livroRepository.saveAndFlush(new Livro("Livro sem notas", "Autor", "Romance"));

		mockMvc.perform(get("/livros/{id}", livro.getId())
						.header(HttpHeaders.AUTHORIZATION, tokenDeLeitor))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mediaNotas").value(0.0))
				.andExpect(jsonPath("$.totalAvaliacoes").value(0));
	}

	@Test
	void naoDeveCadastrarSemEstarLogado() throws Exception {
		mockMvc.perform(post("/livros")
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("Vidas Secas", "Graciliano Ramos", "Romance", null)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void naoDevePermitirQueLeitorCadastreLivro() throws Exception {
		mockMvc.perform(post("/livros")
						.header(HttpHeaders.AUTHORIZATION, tokenDeLeitor)
						.contentType(MediaType.APPLICATION_JSON)
						.content(json("Vidas Secas", "Graciliano Ramos", "Romance", null)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Você não tem permissão para realizar esta ação."));
	}

	@Test
	void deveCadastrarLivro() throws Exception {
		String isbn = isbnDeTeste();

		cadastrar(json("Vidas Secas", "Graciliano Ramos", "Romance", isbn))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.titulo").value("Vidas Secas"))
				.andExpect(jsonPath("$.isbn").value(isbn))
				.andExpect(jsonPath("$.genero").value("Romance"));
	}

	@Test
	void deveGravarIsbnSemHifens() throws Exception {
		String isbn = isbnDeTeste();
		String isbnComHifens = isbn.substring(0, 3) + "-" + isbn.substring(3, 5) + "-" + isbn.substring(5);

		cadastrar(json("Vidas Secas", "Graciliano Ramos", "Romance", isbnComHifens))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.isbn").value(isbn));
	}

	@Test
	void deveCadastrarSemIsbnECamposOpcionaisEmBrancoComoVazios() throws Exception {
		cadastrar("""
				{"titulo": "Livro Sem ISBN", "autor": "Autor", "genero": "Romance",
				 "isbn": "  ", "capaUrl": "", "sinopse": "  "}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.isbn").doesNotExist())
				.andExpect(jsonPath("$.capaUrl").doesNotExist())
				.andExpect(jsonPath("$.sinopse").doesNotExist());
	}

	@Test
	void naoDeveCadastrarIsbnRepetidoMesmoComHifens() throws Exception {
		String isbn = isbnDeTeste();
		cadastrar(json("Vidas Secas", "Graciliano Ramos", "Romance", isbn)).andExpect(status().isCreated());

		String isbnComHifens = isbn.substring(0, 3) + "-" + isbn.substring(3);
		cadastrar(json("Outro Título", "Outro Autor", "Romance", isbnComHifens))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um livro cadastrado com o ISBN: " + isbn));
	}

	@Test
	void deveResponderConflitoQuandoBancoBarraIsbnRepetido() throws Exception {
		// Simula dois cadastros simultâneos: a checagem prévia não vê o primeiro,
		// e quem barra o segundo é a restrição única do banco.
		String isbn = isbnDeTeste();
		Livro primeiro = new Livro("Primeiro", "Autor", "Romance");
		primeiro.setIsbn(isbn);
		livroRepository.saveAndFlush(primeiro);
		doReturn(Optional.empty()).when(livroRepository).findByIsbn(anyString());

		cadastrar(json("Segundo", "Autor", "Romance", isbn))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um livro cadastrado com o ISBN: " + isbn));
	}

	@Test
	void naoDeveCadastrarSemTitulo() throws Exception {
		cadastrar("""
				{"autor": "Graciliano Ramos", "genero": "Romance"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O título é obrigatório."));
	}

	@Test
	void naoDeveCadastrarSemGenero() throws Exception {
		cadastrar(json("Vidas Secas", "Graciliano Ramos", " ", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O gênero é obrigatório."));
	}

	@Test
	void naoDeveCadastrarTituloMaiorQueOPermitido() throws Exception {
		cadastrar(json("a".repeat(256), "Graciliano Ramos", "Romance", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O título deve ter no máximo 255 caracteres."));
	}

	@Test
	void naoDeveCadastrarGeneroMaiorQueOPermitido() throws Exception {
		cadastrar(json("Vidas Secas", "Graciliano Ramos", "g".repeat(51), null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O gênero deve ter no máximo 50 caracteres."));
	}

	@Test
	void naoDeveCadastrarIsbnComQuantidadeDeDigitosInvalida() throws Exception {
		cadastrar(json("Vidas Secas", "Graciliano Ramos", "Romance", "12345"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O ISBN deve ter 10 ou 13 dígitos."));
	}

	@Test
	void naoDeveCadastrarNumeroDePaginasNegativo() throws Exception {
		cadastrar("""
				{"titulo": "Vidas Secas", "autor": "Graciliano Ramos", "genero": "Romance", "numPaginas": -10}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O número de páginas deve ser positivo."));
	}

	@Test
	void deveAtualizarLivro() throws Exception {
		Livro livro = livroRepository.saveAndFlush(new Livro("Titulo Antigo", "Autor Antigo", "Romance"));
		String isbn = isbnDeTeste();

		atualizar(livro.getId(), tokenDeAdmin, """
				{"titulo": "  Vidas Secas  ", "autor": "Graciliano Ramos", "genero": "Romance",
				 "isbn": "%s", "numPaginas": 176}
				""".formatted(isbn))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(livro.getId().toString()))
				.andExpect(jsonPath("$.titulo").value("Vidas Secas"))
				.andExpect(jsonPath("$.autor").value("Graciliano Ramos"))
				.andExpect(jsonPath("$.isbn").value(isbn))
				.andExpect(jsonPath("$.numPaginas").value(176));
	}

	@Test
	void deveAtualizarMantendoOProprioIsbn() throws Exception {
		String isbn = isbnDeTeste();
		Livro livro = new Livro("Vidas Secas", "Graciliano Ramos", "Romance");
		livro.setIsbn(isbn);
		livro = livroRepository.saveAndFlush(livro);

		atualizar(livro.getId(), tokenDeAdmin, json("Vidas Secas (edição revista)", "Graciliano Ramos", "Romance", isbn))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value("Vidas Secas (edição revista)"));
	}

	@Test
	void naoDeveAtualizarComIsbnDeOutroLivro() throws Exception {
		String isbn = isbnDeTeste();
		Livro outro = new Livro("Outro", "Autor", "Romance");
		outro.setIsbn(isbn);
		livroRepository.saveAndFlush(outro);
		Livro livro = livroRepository.saveAndFlush(new Livro("Vidas Secas", "Graciliano Ramos", "Romance"));

		atualizar(livro.getId(), tokenDeAdmin, json("Vidas Secas", "Graciliano Ramos", "Romance", isbn))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um livro cadastrado com o ISBN: " + isbn));
	}

	@Test
	void naoDeveAtualizarLivroInexistente() throws Exception {
		atualizar(UUID.randomUUID(), tokenDeAdmin, json("Vidas Secas", "Graciliano Ramos", "Romance", null))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Livro não encontrado."));
	}

	@Test
	void naoDeveAtualizarComDadosInvalidos() throws Exception {
		Livro livro = livroRepository.saveAndFlush(new Livro("Vidas Secas", "Graciliano Ramos", "Romance"));

		atualizar(livro.getId(), tokenDeAdmin, json(" ", "Graciliano Ramos", "Romance", null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O título é obrigatório."));
	}

	@Test
	void naoDevePermitirQueLeitorAtualizeLivro() throws Exception {
		Livro livro = livroRepository.saveAndFlush(new Livro("Vidas Secas", "Graciliano Ramos", "Romance"));

		atualizar(livro.getId(), tokenDeLeitor, json("Alterado", "Graciliano Ramos", "Romance", null))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Você não tem permissão para realizar esta ação."));
	}

	private ResultActions atualizar(UUID id, String token, String corpo) throws Exception {
		return mockMvc.perform(put("/livros/{id}", id)
				.header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

	private ResultActions cadastrar(String corpo) throws Exception {
		return mockMvc.perform(post("/livros")
				.header(HttpHeaders.AUTHORIZATION, tokenDeAdmin)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

	private static String json(String titulo, String autor, String genero, String isbn) {
		String isbnJson = isbn == null ? "null" : "\"" + isbn + "\"";
		return """
				{"titulo": "%s", "autor": "%s", "genero": "%s", "isbn": %s}
				""".formatted(titulo, autor, genero, isbnJson);
	}

	private Usuario novoUsuario(TipoConta tipoConta) {
		String email = "teste-" + UUID.randomUUID().toString().substring(0, 8) + "@bookverse.com";
		return usuarioRepository.save(new Usuario("Usuário Teste", email, "hash-ficticio",
				LocalDate.of(2000, 1, 15), tipoConta));
	}

	private String tokenDe(Usuario usuario) {
		return "Bearer " + jwtService.gerarToken(usuario).token();
	}

	/** ISBN de 13 dígitos começando com 999 para nunca colidir com livros reais do banco. */
	private static String isbnDeTeste() {
		return "999" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
	}
}
