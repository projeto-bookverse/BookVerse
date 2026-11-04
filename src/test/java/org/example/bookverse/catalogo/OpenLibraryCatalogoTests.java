package org.example.bookverse.catalogo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Testa a conversão da resposta da Open Library em LivroDTO sem acessar a internet:
 * o MockRestServiceServer intercepta a chamada HTTP e devolve um JSON salvo em src/test/resources/openlibrary.
 */
class OpenLibraryCatalogoTests {

	private static final String URL_HOBBIT =
			"https://openlibrary.org/api/books?bibkeys=ISBN%3A9780547928227&format=json&jscmd=data";

	private MockRestServiceServer servidor;

	private OpenLibraryCatalogo catalogo;

	@BeforeEach
	void configurar() {
		RestClient.Builder builder = RestClient.builder();
		servidor = MockRestServiceServer.bindTo(builder).build();
		catalogo = new OpenLibraryCatalogo(builder);
	}

	@Test
	void deveConverterRespostaEmLivroDTO() {
		servidor.expect(requestTo(URL_HOBBIT))
				.andRespond(withSuccess(new ClassPathResource("openlibrary/isbn-9780547928227.json"),
						MediaType.APPLICATION_JSON));

		LivroDTO livro = catalogo.buscarPorIsbn("978-0-547-92822-7").orElseThrow();

		assertEquals("The Hobbit", livro.titulo());
		assertEquals("J.R.R. Tolkien", livro.autor());
		assertEquals("9780547928227", livro.isbn());
		assertEquals("Fantasia", livro.genero());
		assertEquals(2012, livro.anoPublicacao());
		assertEquals(300, livro.numPaginas());
		assertEquals("https://covers.openlibrary.org/b/id/12003329-L.jpg", livro.capaUrl());
		assertNull(livro.sinopse());
		servidor.verify();
	}

	@Test
	void deveRetornarVazioQuandoIsbnNaoExiste() {
		servidor.expect(requestTo("https://openlibrary.org/api/books?bibkeys=ISBN%3A0000000000000&format=json&jscmd=data"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		Optional<LivroDTO> livro = catalogo.buscarPorIsbn("0000000000000");

		assertTrue(livro.isEmpty());
	}

	/**
	 * Chama a API de verdade. Desativado por padrão para não depender da internet;
	 * para rodar: mvnw test -Dtest=OpenLibraryCatalogoTests -Dbookverse.online=true
	 */
	@Test
	@EnabledIfSystemProperty(named = "bookverse.online", matches = "true")
	void deveBuscarNaApiReal() {
		OpenLibraryCatalogo catalogoReal = new OpenLibraryCatalogo(RestClient.builder());

		LivroDTO livro = catalogoReal.buscarPorIsbn("9780547928227").orElseThrow();

		assertEquals("The Hobbit", livro.titulo());
	}
}
