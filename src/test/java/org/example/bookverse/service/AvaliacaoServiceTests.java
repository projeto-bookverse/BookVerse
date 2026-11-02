package org.example.bookverse.service;

import org.example.bookverse.dto.AvaliacaoAtualizacaoRequest;
import org.example.bookverse.dto.AvaliacaoCadastroRequest;
import org.example.bookverse.dto.AvaliacaoResponse;
import org.example.bookverse.entity.Avaliacao;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.exception.AcessoAvaliacaoNegadoException;
import org.example.bookverse.exception.AvaliacaoDuplicadaException;
import org.example.bookverse.exception.RecursoAvaliacaoNaoEncontradoException;
import org.example.bookverse.repository.AvaliacaoRepository;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvaliacaoServiceTests {

	@Mock
	private AvaliacaoRepository avaliacaoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private LivroRepository livroRepository;

	@InjectMocks
	private AvaliacaoService avaliacaoService;

	@Test
	void deveCadastrarAvaliacaoComUsuarioELivroExistentes() {
		UUID usuarioId = UUID.randomUUID();
		UUID livroId = UUID.randomUUID();
		Usuario usuario = usuario(usuarioId);
		Livro livro = livro(livroId);
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
		when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
		when(avaliacaoRepository.existsByUsuarioIdAndLivroId(usuarioId, livroId)).thenReturn(false);
		when(avaliacaoRepository.saveAndFlush(any(Avaliacao.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		AvaliacaoResponse response = avaliacaoService.cadastrar(
				new AvaliacaoCadastroRequest(usuarioId, livroId, (short) 5, "  Excelente!  ", true));

		assertEquals(usuarioId, response.usuarioId());
		assertEquals(livroId, response.livroId());
		assertEquals("Excelente!", response.textoResenha());
		assertEquals((short) 5, response.nota());
		assertEquals(true, response.flagSpoiler());
	}

	@Test
	void deveRejeitarCadastroComUsuarioInexistente() {
		UUID usuarioId = UUID.randomUUID();
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

		assertThrows(RecursoAvaliacaoNaoEncontradoException.class,
				() -> avaliacaoService.cadastrar(cadastro(usuarioId, UUID.randomUUID())));
		verify(livroRepository, never()).findById(any());
	}

	@Test
	void deveRejeitarCadastroComLivroInexistente() {
		UUID usuarioId = UUID.randomUUID();
		UUID livroId = UUID.randomUUID();
		Usuario usuario = usuario(usuarioId);
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
		when(livroRepository.findById(livroId)).thenReturn(Optional.empty());

		assertThrows(RecursoAvaliacaoNaoEncontradoException.class,
				() -> avaliacaoService.cadastrar(cadastro(usuarioId, livroId)));
		verify(avaliacaoRepository, never()).saveAndFlush(any());
	}

	@Test
	void deveRejeitarAvaliacaoDuplicadaAntesDaPersistencia() {
		UUID usuarioId = UUID.randomUUID();
		UUID livroId = UUID.randomUUID();
		Usuario usuario = usuario(usuarioId);
		Livro livro = livro(livroId);
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
		when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
		when(avaliacaoRepository.existsByUsuarioIdAndLivroId(usuarioId, livroId)).thenReturn(true);

		assertThrows(AvaliacaoDuplicadaException.class,
				() -> avaliacaoService.cadastrar(cadastro(usuarioId, livroId)));
		verify(avaliacaoRepository, never()).saveAndFlush(any());
	}

	@Test
	void deveConverterViolacaoUnicaConcorrenteEmAvaliacaoDuplicada() {
		UUID usuarioId = UUID.randomUUID();
		UUID livroId = UUID.randomUUID();
		Usuario usuario = usuario(usuarioId);
		Livro livro = livro(livroId);
		when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
		when(livroRepository.findById(livroId)).thenReturn(Optional.of(livro));
		when(avaliacaoRepository.existsByUsuarioIdAndLivroId(usuarioId, livroId)).thenReturn(false);
		SQLException causa = new SQLException("duplicate key uq_avaliacao_usuario_livro", "23505");
		when(avaliacaoRepository.saveAndFlush(any(Avaliacao.class)))
				.thenThrow(new DataIntegrityViolationException("violação única", causa));

		assertThrows(AvaliacaoDuplicadaException.class,
				() -> avaliacaoService.cadastrar(cadastro(usuarioId, livroId)));
	}

	@Test
	void deveListarAvaliacoesDeLivroExistente() {
		UUID livroId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao();
		when(livroRepository.existsById(livroId)).thenReturn(true);
		when(avaliacaoRepository.findByLivroIdOrderByCriadoEmDesc(livroId)).thenReturn(List.of(avaliacao));

		assertEquals(1, avaliacaoService.listarPorLivro(livroId).size());
	}

	@Test
	void deveRejeitarConsultaDeLivroInexistente() {
		UUID livroId = UUID.randomUUID();
		when(livroRepository.existsById(livroId)).thenReturn(false);

		assertThrows(RecursoAvaliacaoNaoEncontradoException.class,
				() -> avaliacaoService.listarPorLivro(livroId));
	}

	@Test
	void deveListarAvaliacoesDeUsuarioExistente() {
		UUID usuarioId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao();
		when(usuarioRepository.existsById(usuarioId)).thenReturn(true);
		when(avaliacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)).thenReturn(List.of(avaliacao));

		assertEquals(1, avaliacaoService.listarPorUsuario(usuarioId).size());
	}

	@Test
	void deveBuscarAvaliacaoPorId() {
		UUID avaliacaoId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao(avaliacaoId, UUID.randomUUID());
		when(avaliacaoRepository.findById(avaliacaoId)).thenReturn(Optional.of(avaliacao));

		assertEquals(avaliacaoId, avaliacaoService.buscarPorId(avaliacaoId).id());
	}

	@Test
	void deveRejeitarConsultaDeAvaliacaoInexistente() {
		UUID avaliacaoId = UUID.randomUUID();
		when(avaliacaoRepository.findById(avaliacaoId)).thenReturn(Optional.empty());

		assertThrows(RecursoAvaliacaoNaoEncontradoException.class,
				() -> avaliacaoService.buscarPorId(avaliacaoId));
	}

	@Test
	void deveAtualizarAvaliacaoDoProprioUsuario() {
		UUID avaliacaoId = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao(avaliacaoId, usuarioId);
		when(avaliacaoRepository.findById(avaliacaoId)).thenReturn(Optional.of(avaliacao));
		when(avaliacaoRepository.saveAndFlush(avaliacao)).thenReturn(avaliacao);

		AvaliacaoResponse response = avaliacaoService.atualizar(avaliacaoId,
				new AvaliacaoAtualizacaoRequest(usuarioId, (short) 4, "Nova resenha", false));

		assertEquals((short) 4, response.nota());
		assertEquals("Nova resenha", response.textoResenha());
		assertEquals(false, response.flagSpoiler());
	}

	@Test
	void deveImpedirAtualizacaoPorOutroUsuario() {
		UUID avaliacaoId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao(avaliacaoId, UUID.randomUUID());
		when(avaliacaoRepository.findById(avaliacaoId))
				.thenReturn(Optional.of(avaliacao));

		assertThrows(AcessoAvaliacaoNegadoException.class,
				() -> avaliacaoService.atualizar(avaliacaoId,
						new AvaliacaoAtualizacaoRequest(UUID.randomUUID(), (short) 4, "Alteração", false)));
		verify(avaliacaoRepository, never()).saveAndFlush(any());
	}

	@Test
	void deveExcluirAvaliacaoDoProprioUsuario() {
		UUID avaliacaoId = UUID.randomUUID();
		UUID usuarioId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao(avaliacaoId, usuarioId);
		when(avaliacaoRepository.findById(avaliacaoId)).thenReturn(Optional.of(avaliacao));

		avaliacaoService.excluir(avaliacaoId, usuarioId);

		verify(avaliacaoRepository).delete(avaliacao);
	}

	@Test
	void deveImpedirExclusaoPorOutroUsuario() {
		UUID avaliacaoId = UUID.randomUUID();
		Avaliacao avaliacao = avaliacao(avaliacaoId, UUID.randomUUID());
		when(avaliacaoRepository.findById(avaliacaoId))
				.thenReturn(Optional.of(avaliacao));

		assertThrows(AcessoAvaliacaoNegadoException.class,
				() -> avaliacaoService.excluir(avaliacaoId, UUID.randomUUID()));
		verify(avaliacaoRepository, never()).delete(any(Avaliacao.class));
	}

	private AvaliacaoCadastroRequest cadastro(UUID usuarioId, UUID livroId) {
		return new AvaliacaoCadastroRequest(usuarioId, livroId, (short) 5, "Muito bom", false);
	}

	private Usuario usuario(UUID id) {
		Usuario usuario = new Usuario("Leitora", "leitora@example.com", "hash",
				LocalDate.of(2000, 1, 1), TipoConta.LEITOR);
		ReflectionTestUtils.setField(usuario, "id", id);
		return usuario;
	}

	private Livro livro(UUID id) {
		Livro livro = new Livro("Livro teste", "Autor teste", "Ficção");
		ReflectionTestUtils.setField(livro, "id", id);
		return livro;
	}

	private Avaliacao avaliacao() {
		return avaliacao(UUID.randomUUID(), UUID.randomUUID());
	}

	private Avaliacao avaliacao(UUID avaliacaoId, UUID usuarioId) {
		Avaliacao avaliacao = new Avaliacao(usuario(usuarioId), livro(UUID.randomUUID()), (short) 5);
		ReflectionTestUtils.setField(avaliacao, "id", avaliacaoId);
		ReflectionTestUtils.setField(avaliacao, "criadoEm", LocalDateTime.now());
		ReflectionTestUtils.setField(avaliacao, "atualizadoEm", LocalDateTime.now());
		avaliacao.setTextoResenha("Resenha existente");
		return avaliacao;
	}
}
