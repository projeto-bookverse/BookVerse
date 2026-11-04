package org.example.bookverse.controller;

import org.example.bookverse.entity.Livro;
import org.example.bookverse.repository.LivroRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o cadastro de livros de ponta a ponta (HTTP → Service → PostgreSQL local).
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

	private ResultActions cadastrar(String corpo) throws Exception {
		return mockMvc.perform(post("/livros")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

	private static String json(String titulo, String autor, String genero, String isbn) {
		String isbnJson = isbn == null ? "null" : "\"" + isbn + "\"";
		return """
				{"titulo": "%s", "autor": "%s", "genero": "%s", "isbn": %s}
				""".formatted(titulo, autor, genero, isbnJson);
	}

	/** ISBN de 13 dígitos começando com 999 para nunca colidir com livros reais do banco. */
	private static String isbnDeTeste() {
		return "999" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
	}
}
