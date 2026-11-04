package org.example.bookverse.service;

import org.example.bookverse.dto.AvaliacaoAtualizacaoRequest;
import org.example.bookverse.dto.AvaliacaoCadastroRequest;
import org.example.bookverse.dto.AvaliacaoResponse;
import org.example.bookverse.entity.Avaliacao;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.exception.AcessoNegadoException;
import org.example.bookverse.exception.ConflitoException;
import org.example.bookverse.exception.RecursoNaoEncontradoException;
import org.example.bookverse.repository.AvaliacaoRepository;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AvaliacaoService {

	private static final String RESTRICAO_AVALIACAO_UNICA = "uq_avaliacao_usuario_livro";
	private static final String MENSAGEM_AVALIACAO_DUPLICADA = "Este usuário já avaliou este livro.";
	private static final String MENSAGEM_USUARIO_NAO_ENCONTRADO = "Usuário não encontrado.";
	private static final String MENSAGEM_LIVRO_NAO_ENCONTRADO = "Livro não encontrado.";

	private final AvaliacaoRepository avaliacaoRepository;
	private final UsuarioRepository usuarioRepository;
	private final LivroRepository livroRepository;

	public AvaliacaoService(
			AvaliacaoRepository avaliacaoRepository,
			UsuarioRepository usuarioRepository,
			LivroRepository livroRepository) {
		this.avaliacaoRepository = avaliacaoRepository;
		this.usuarioRepository = usuarioRepository;
		this.livroRepository = livroRepository;
	}

	@Transactional
	public AvaliacaoResponse cadastrar(AvaliacaoCadastroRequest request) {
		Usuario usuario = usuarioRepository.findById(request.usuarioId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(MENSAGEM_USUARIO_NAO_ENCONTRADO));
		Livro livro = livroRepository.findById(request.livroId())
				.orElseThrow(() -> new RecursoNaoEncontradoException(MENSAGEM_LIVRO_NAO_ENCONTRADO));

		if (avaliacaoRepository.existsByUsuarioIdAndLivroId(usuario.getId(), livro.getId())) {
			throw new ConflitoException(MENSAGEM_AVALIACAO_DUPLICADA);
		}

		Avaliacao avaliacao = new Avaliacao(usuario, livro, request.nota());
		avaliacao.setTextoResenha(textoOpcional(request.textoResenha()));
		avaliacao.setFlagSpoiler(request.flagSpoiler());

		try {
			return AvaliacaoResponse.from(avaliacaoRepository.saveAndFlush(avaliacao));
		} catch (DataIntegrityViolationException exception) {
			if (isViolacaoDaAvaliacaoUnica(exception)) {
				throw new ConflitoException(MENSAGEM_AVALIACAO_DUPLICADA);
			}
			throw exception;
		}
	}

	@Transactional(readOnly = true)
	public List<AvaliacaoResponse> listarPorLivro(UUID livroId) {
		if (!livroRepository.existsById(livroId)) {
			throw new RecursoNaoEncontradoException(MENSAGEM_LIVRO_NAO_ENCONTRADO);
		}
		return avaliacaoRepository.findByLivroIdOrderByCriadoEmDesc(livroId).stream()
				.map(AvaliacaoResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<AvaliacaoResponse> listarPorUsuario(UUID usuarioId) {
		if (!usuarioRepository.existsById(usuarioId)) {
			throw new RecursoNaoEncontradoException(MENSAGEM_USUARIO_NAO_ENCONTRADO);
		}
		return avaliacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId).stream()
				.map(AvaliacaoResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public AvaliacaoResponse buscarPorId(UUID id) {
		return AvaliacaoResponse.from(buscarAvaliacao(id));
	}

	@Transactional
	public AvaliacaoResponse atualizar(UUID id, AvaliacaoAtualizacaoRequest request) {
		Avaliacao avaliacao = buscarAvaliacao(id);
		validarTitularidade(avaliacao, request.usuarioId());

		avaliacao.setNota(request.nota());
		avaliacao.setTextoResenha(textoOpcional(request.textoResenha()));
		avaliacao.setFlagSpoiler(request.flagSpoiler());
		return AvaliacaoResponse.from(avaliacaoRepository.saveAndFlush(avaliacao));
	}

	@Transactional
	public void excluir(UUID id, UUID usuarioId) {
		Avaliacao avaliacao = buscarAvaliacao(id);
		validarTitularidade(avaliacao, usuarioId);
		avaliacaoRepository.delete(avaliacao);
	}

	/** A resenha é opcional (RN19): texto em branco é gravado como vazio. */
	private static String textoOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private Avaliacao buscarAvaliacao(UUID id) {
		return avaliacaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada."));
	}

	private void validarTitularidade(Avaliacao avaliacao, UUID usuarioId) {
		if (!avaliacao.getUsuario().getId().equals(usuarioId)) {
			throw new AcessoNegadoException("Somente o autor da avaliação pode alterá-la ou excluí-la.");
		}
	}

	private boolean isViolacaoDaAvaliacaoUnica(DataIntegrityViolationException exception) {
		boolean violacaoUnica = false;
		boolean restricaoDaAvaliacao = false;
		Throwable causa = exception;

		while (causa != null) {
			if (causa instanceof ConstraintViolationException violation) {
				String nomeRestricao = violation.getConstraintName();
				if (nomeRestricao != null
						&& RESTRICAO_AVALIACAO_UNICA.equals(nomeRestricao.toLowerCase(Locale.ROOT))) {
					restricaoDaAvaliacao = true;
				}
			}
			if (causa instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
				violacaoUnica = true;
				String mensagem = sqlException.getMessage();
				if (mensagem != null
						&& mensagem.toLowerCase(Locale.ROOT).contains(RESTRICAO_AVALIACAO_UNICA)) {
					restricaoDaAvaliacao = true;
				}
			}
			causa = causa.getCause();
		}
		return violacaoUnica && restricaoDaAvaliacao;
	}
}
