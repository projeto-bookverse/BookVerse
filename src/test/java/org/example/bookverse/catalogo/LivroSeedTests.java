package org.example.bookverse.catalogo;

import org.example.bookverse.repository.LivroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.Commit;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a carga inicial de livros a partir de src/test/resources/seed/livros.json.
 * Os testes normais sofrem rollback. O teste carregarSeedNoBanco grava de verdade (@Commit),
 * mas só roda quando pedido: mvnw test -Dtest=LivroSeedTests -Dbookverse.seed=true
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LivroSeedTests {

	private static final String ARQUIVO_SEED = "seed/livros.json";

	@Autowired
	private LivroRepository livroRepository;

	private LivroImportador importador;

	@BeforeEach
	void configurar() {
		importador = new LivroImportador(livroRepository);
	}

	@Test
	void deveLerArquivoDeSeedComCamposObrigatorios() throws IOException {
		List<LivroDTO> livros = lerSeed();

		assertFalse(livros.isEmpty());
		for (LivroDTO livro : livros) {
			assertNotNull(livro.titulo());
			assertNotNull(livro.autor());
			assertNotNull(livro.genero(), "Livro sem gênero: " + livro.titulo());
		}
	}

	@Test
	void deveImportarSeedSemDuplicarAoRodarDuasVezes() throws IOException {
		List<LivroDTO> livros = lerSeed();
		long totalAntes = livroRepository.count();

		importador.importarTodos(livros);
		livroRepository.flush();
		long totalDepoisDaPrimeira = livroRepository.count();

		importador.importarTodos(livros);
		livroRepository.flush();

		assertTrue(totalDepoisDaPrimeira <= totalAntes + livros.size());
		assertEquals(totalDepoisDaPrimeira, livroRepository.count());
	}

	@Test
	@Commit
	@EnabledIfSystemProperty(named = "bookverse.seed", matches = "true")
	void carregarSeedNoBanco() throws IOException {
		List<LivroDTO> livros = lerSeed();

		importador.importarTodos(livros);

		for (LivroDTO livro : livros) {
			if (livro.isbn() != null) {
				assertTrue(livroRepository.findByIsbn(livro.isbn()).isPresent());
			}
		}
	}

	private List<LivroDTO> lerSeed() throws IOException {
		try (InputStream arquivo = new ClassPathResource(ARQUIVO_SEED).getInputStream()) {
			return JsonMapper.builder().build().readValue(arquivo, new TypeReference<>() {
			});
		}
	}
}
