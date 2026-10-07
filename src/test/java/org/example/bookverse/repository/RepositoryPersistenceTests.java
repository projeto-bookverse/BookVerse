package org.example.bookverse.repository;

import org.example.bookverse.entity.Avaliacao;
import org.example.bookverse.entity.EstanteUsuario;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.StatusLeitura;
import org.example.bookverse.entity.enums.TipoConta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a gravação de registros no PostgreSQL local (bookverse).
 * Cada teste roda em uma transação que sofre rollback ao final, então nada fica salvo no banco,
 * a não ser que seja adicionado "@commit" no teste.
 * O ddl-auto=validate faz o Hibernate conferir se as entidades batem com as tabelas existentes.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryPersistenceTests {

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private LivroRepository livroRepository;

	@Autowired
	private EstanteUsuarioRepository estanteRepository;

	@Autowired
	private AvaliacaoRepository avaliacaoRepository;

	@Test
	void deveSalvarUsuario() {
		Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());

		assertNotNull(usuario.getId());
		assertNotNull(usuario.getDataCadastro());

		Usuario encontrado = usuarioRepository.findByEmail(usuario.getEmail()).orElseThrow();
		assertEquals(TipoConta.LEITOR, encontrado.getTipoConta());
	}

	@Test
	void deveSalvarLivro() {
		Livro livro = novoLivro();
		livro.setSinopse("Um romance sobre a vida no sertão.");
		livro.setAnoPublicacao(1938);
		livro.setNumPaginas(176);

		Livro salvo = livroRepository.saveAndFlush(livro);

		assertNotNull(salvo.getId());
		assertTrue(livroRepository.findByIsbn(livro.getIsbn()).isPresent());
	}

	@Test
	void deveSalvarLivroNaEstanteDoUsuario() {
		Usuario usuario = usuarioRepository.save(novoUsuario());
		Livro livro = livroRepository.save(novoLivro());

		EstanteUsuario estante = new EstanteUsuario(usuario, livro, StatusLeitura.QUERO_LER);
		estante.setDataInicio(LocalDate.now());
		estanteRepository.saveAndFlush(estante);

		assertNotNull(estante.getId());
		assertEquals(1, estanteRepository.findByUsuarioId(usuario.getId()).size());
	}

	@Test
	void deveSalvarAvaliacao() {
		Usuario usuario = usuarioRepository.save(novoUsuario());
		Livro livro = livroRepository.save(novoLivro());

		Avaliacao avaliacao = new Avaliacao(usuario, livro, (short) 5);
		avaliacao.setTextoResenha("Excelente!");
		avaliacaoRepository.saveAndFlush(avaliacao);

		assertNotNull(avaliacao.getId());
		assertNotNull(avaliacao.getCriadoEm());
		assertFalse(avaliacao.isFlagSpoiler());
		assertEquals(1, avaliacaoRepository.findByLivroId(livro.getId()).size());
	}

	@Test
	void naoDeveSalvarAvaliacaoComNotaForaDoIntervalo() {
		Usuario usuario = usuarioRepository.save(novoUsuario());
		Livro livro = livroRepository.save(novoLivro());

		Avaliacao avaliacao = new Avaliacao(usuario, livro, (short) 6);

		assertThrows(DataIntegrityViolationException.class, () -> avaliacaoRepository.saveAndFlush(avaliacao));
	}

	@Test
	void naoDeveSalvarMesmoLivroDuasVezesNaEstante() {
		Usuario usuario = usuarioRepository.save(novoUsuario());
		Livro livro = livroRepository.save(novoLivro());
		estanteRepository.saveAndFlush(new EstanteUsuario(usuario, livro, StatusLeitura.LENDO));

		EstanteUsuario duplicado = new EstanteUsuario(usuario, livro, StatusLeitura.LIDO);

		assertThrows(DataIntegrityViolationException.class, () -> estanteRepository.saveAndFlush(duplicado));
	}

	@Test
	void naoDeveSalvarUsuarioComEmailDuplicado() {
		Usuario usuario = usuarioRepository.saveAndFlush(novoUsuario());

		Usuario duplicado = novoUsuario();
		duplicado.setEmail(usuario.getEmail());

		assertThrows(DataIntegrityViolationException.class, () -> usuarioRepository.saveAndFlush(duplicado));
	}

	private Usuario novoUsuario() {
		String sufixo = UUID.randomUUID().toString().substring(0, 8);
		return new Usuario("Leitor Teste", "teste-" + sufixo + "@bookverse.com", "hash-ficticio",
				LocalDate.of(2000, 1, 15), TipoConta.LEITOR);
	}

	private Livro novoLivro() {
		Livro livro = new Livro("Vidas Secas", "Graciliano Ramos", "Romance");
		livro.setIsbn("TST-" + UUID.randomUUID().toString().substring(0, 12));
		return livro;
	}
}
