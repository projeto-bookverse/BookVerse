package org.example.bookverse.catalogo;

import org.example.bookverse.entity.Livro;
import org.example.bookverse.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testa as regras de gravação do LivroImportador (protótipo do LivroService) no PostgreSQL local.
 * Assim como em RepositoryPersistenceTests, cada teste sofre rollback ao final e nada fica salvo.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LivroImportadorTests {

	@Autowired
	private LivroRepository livroRepository;

	private LivroImportador importador;

	@BeforeEach
	void configurar() {
		importador = new LivroImportador(livroRepository);
	}

	@Test
	void deveCriarLivroNovo() {
		String isbn = isbnDeTeste();

		Livro livro = importador.importar(novoLivro("Vidas Secas", isbn, "Romance"));

		assertNotNull(livro.getId());
		assertEquals("Romance", livroRepository.findByIsbn(isbn).orElseThrow().getGenero());
	}

	@Test
	void deveAtualizarEmVezDeDuplicarQuandoIsbnJaExiste() {
		String isbn = isbnDeTeste();
		Livro original = importador.importar(novoLivro("Vidas Secas", isbn, "Romance"));
		long totalAntes = livroRepository.count();

		Livro atualizado = importador.importar(novoLivro("Vidas Secas (edição revisada)", isbn, "Romance"));

		assertEquals(original.getId(), atualizado.getId());
		assertEquals(totalAntes, livroRepository.count());
		assertEquals("Vidas Secas (edição revisada)", livroRepository.findByIsbn(isbn).orElseThrow().getTitulo());
	}

	@Test
	void deveTratarIsbnComHifensComoOMesmoLivro() {
		String isbn = isbnDeTeste();
		String isbnComHifens = isbn.substring(0, 3) + "-" + isbn.substring(3, 5) + "-" + isbn.substring(5);
		Livro original = importador.importar(novoLivro("Vidas Secas", isbn, "Romance"));

		Livro mesmo = importador.importar(novoLivro("Vidas Secas", isbnComHifens, "Romance"));

		assertEquals(original.getId(), mesmo.getId());
		assertEquals(isbn, mesmo.getIsbn());
	}

	@Test
	void deveEvitarDuplicataPorTituloEAutorQuandoNaoHaIsbn() {
		String titulo = "Livro Sem ISBN " + isbnDeTeste();
		Livro original = importador.importar(novoLivro(titulo, null, "Romance"));

		Livro mesmo = importador.importar(novoLivro(titulo.toUpperCase(), null, "Romance"));

		assertEquals(original.getId(), mesmo.getId());
	}

	@Test
	void deveUsarGeneroPadraoQuandoGeneroNaoInformado() {
		Livro livro = importador.importar(novoLivro("Livro Sem Gênero", isbnDeTeste(), null));

		assertEquals(GeneroNormalizer.GENERO_PADRAO, livro.getGenero());
	}

	@Test
	void naoDeveImportarLivroSemTitulo() {
		LivroDTO semTitulo = novoLivro(" ", isbnDeTeste(), "Romance");

		assertThrows(IllegalArgumentException.class, () -> importador.importar(semTitulo));
	}

	private LivroDTO novoLivro(String titulo, String isbn, String genero) {
		return new LivroDTO(titulo, "Graciliano Ramos", isbn, null, null, genero, 1938, 176);
	}

	/** ISBN de 13 dígitos começando com 999 para nunca colidir com livros reais do banco. */
	private String isbnDeTeste() {
		return "999" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
	}
}
