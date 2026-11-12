package org.example.bookverse.service;

import org.example.bookverse.dto.LivroRequest;
import org.example.bookverse.dto.LivroResponse;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.exception.ConflitoException;
import org.example.bookverse.exception.DadosInvalidosException;
import org.example.bookverse.exception.RecursoNaoEncontradoException;
import org.example.bookverse.repository.AvaliacaoRepository;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.projection.ResumoAvaliacoesLivro;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class LivroService {
	private static final int LIVROS_POR_PAGINA = 20;

	// Limites iguais aos das colunas da tabela livros
	private static final int TAMANHO_MAXIMO_TITULO = 255;
	private static final int TAMANHO_MAXIMO_AUTOR = 150;
	private static final int TAMANHO_MAXIMO_GENERO = 50;
	private static final int TAMANHO_MAXIMO_CAPA_URL = 255;

	private static final String RESTRICAO_ISBN_UNICO = "livros_isbn_key";
	private static final String MENSAGEM_ISBN_CADASTRADO = "Já existe um livro cadastrado com o ISBN: ";
	private static final String MENSAGEM_LIVRO_NAO_ENCONTRADO = "Livro não encontrado.";

	private final LivroRepository livroRepository;
	private final AvaliacaoRepository avaliacaoRepository;

	public LivroService(LivroRepository livroRepository, AvaliacaoRepository avaliacaoRepository) {
		this.livroRepository = livroRepository;
		this.avaliacaoRepository = avaliacaoRepository;
	}

	@Transactional(readOnly = true)
	public Page<LivroResponse> listar(int pagina) {
		if (pagina < 0) {
			throw new DadosInvalidosException("O número da página não pode ser negativo.");
		}

		Page<Livro> livros = livroRepository.findAll(PageRequest.of(pagina, LIVROS_POR_PAGINA));
		Map<UUID, ResumoAvaliacoesLivro> resumos = buscarResumos(livros.getContent());
		return livros.map(livro -> respostaComResumo(livro, resumos.get(livro.getId())));
	}

	@Transactional(readOnly = true)
	public LivroResponse buscarPorId(UUID id) {
		Livro livro = livroRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException(MENSAGEM_LIVRO_NAO_ENCONTRADO));
		Double media = avaliacaoRepository.calcularMediaNotasPorLivroId(id);
		long total = avaliacaoRepository.countByLivroId(id);
		return LivroResponse.from(livro, media, total);
	}

	private Map<UUID, ResumoAvaliacoesLivro> buscarResumos(List<Livro> livros) {
		if (livros.isEmpty()) {
			return Map.of();
		}
		List<UUID> ids = livros.stream().map(Livro::getId).toList();
		Map<UUID, ResumoAvaliacoesLivro> resumos = new HashMap<>();
		for (ResumoAvaliacoesLivro resumo : avaliacaoRepository.resumirPorLivroIds(ids)) {
			resumos.put(resumo.getLivroId(), resumo);
		}
		return resumos;
	}

	private static LivroResponse respostaComResumo(Livro livro, ResumoAvaliacoesLivro resumo) {
		if (resumo == null) {
			return LivroResponse.from(livro);
		}
		return LivroResponse.from(livro, resumo.getMediaNotas(), resumo.getTotalAvaliacoes());
	}

	@Transactional
	public LivroResponse cadastrar(LivroRequest request) {
		validar(request);

		String isbn = normalizarIsbn(request.isbn());
		if (isbn != null && livroRepository.findByIsbn(isbn).isPresent()) {
			throw new ConflitoException(MENSAGEM_ISBN_CADASTRADO + isbn);
		}

		Livro livro = new Livro(
				request.titulo().trim(),
				request.autor().trim(),
				request.genero().trim());
		preencher(livro, request, isbn);
		return salvar(livro, isbn);
	}

	/** Substitui todos os dados do livro. Campos opcionais não enviados ficam vazios. */
	@Transactional
	public LivroResponse atualizar(UUID id, LivroRequest request) {
		validar(request);

		Livro livro = livroRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException(MENSAGEM_LIVRO_NAO_ENCONTRADO));

		String isbn = normalizarIsbn(request.isbn());
		if (isbn != null && livroRepository.findByIsbn(isbn)
				.filter(outro -> !outro.getId().equals(id))
				.isPresent()) {
			throw new ConflitoException(MENSAGEM_ISBN_CADASTRADO + isbn);
		}

		livro.setTitulo(request.titulo().trim());
		livro.setAutor(request.autor().trim());
		livro.setGenero(request.genero().trim());
		preencher(livro, request, isbn);
		return salvar(livro, isbn);
	}

	private static void preencher(Livro livro, LivroRequest request, String isbn) {
		livro.setIsbn(isbn);
		livro.setCapaUrl(textoOpcional(request.capaUrl()));
		livro.setSinopse(textoOpcional(request.sinopse()));
		livro.setAnoPublicacao(request.anoPublicacao());
		livro.setNumPaginas(request.numPaginas());
	}

	private LivroResponse salvar(Livro livro, String isbn) {
		try {
			return LivroResponse.from(livroRepository.saveAndFlush(livro));
		} catch (DataIntegrityViolationException exception) {
			// Duas gravações simultâneas com o mesmo ISBN passam pela checagem prévia;
			// a segunda é barrada pela restrição única do banco.
			if (violouRestricao(exception, RESTRICAO_ISBN_UNICO)) {
				throw new ConflitoException(MENSAGEM_ISBN_CADASTRADO + isbn);
			}
			throw exception;
		}
	}

	/**
	 * Remove hífens e espaços, para que "978-85-359-1066-7" e "9788535910667" sejam o mesmo livro.
	 * Devolve null quando o ISBN não foi informado.
	 */
	public static String normalizarIsbn(String isbn) {
		if (estaVazio(isbn)) {
			return null;
		}
		return isbn.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
	}

	private void validar(LivroRequest request) {
		if (request == null) {
			throw new DadosInvalidosException("Os dados do livro não foram enviados.");
		}
		validarTextoObrigatorio(request.titulo(), "O título", TAMANHO_MAXIMO_TITULO);
		validarTextoObrigatorio(request.autor(), "O autor", TAMANHO_MAXIMO_AUTOR);
		validarTextoObrigatorio(request.genero(), "O gênero", TAMANHO_MAXIMO_GENERO);

		String isbn = normalizarIsbn(request.isbn());
		if (isbn != null && isbn.length() != 10 && isbn.length() != 13) {
			throw new DadosInvalidosException("O ISBN deve ter 10 ou 13 dígitos.");
		}
		String capaUrl = textoOpcional(request.capaUrl());
		if (capaUrl != null && capaUrl.length() > TAMANHO_MAXIMO_CAPA_URL) {
			throw new DadosInvalidosException(
					"O endereço da capa deve ter no máximo " + TAMANHO_MAXIMO_CAPA_URL + " caracteres.");
		}
		if (request.anoPublicacao() != null && request.anoPublicacao() <= 0) {
			throw new DadosInvalidosException("O ano de publicação deve ser positivo.");
		}
		if (request.numPaginas() != null && request.numPaginas() <= 0) {
			throw new DadosInvalidosException("O número de páginas deve ser positivo.");
		}
	}

	private static void validarTextoObrigatorio(String valor, String campo, int tamanhoMaximo) {
		if (estaVazio(valor)) {
			throw new DadosInvalidosException(campo + " é obrigatório.");
		}
		if (valor.trim().length() > tamanhoMaximo) {
			throw new DadosInvalidosException(campo + " deve ter no máximo " + tamanhoMaximo + " caracteres.");
		}
	}

	private static boolean estaVazio(String valor) {
		return valor == null || valor.isBlank();
	}

	private static String textoOpcional(String valor) {
		return estaVazio(valor) ? null : valor.trim();
	}

	private static boolean violouRestricao(Throwable exception, String nomeRestricao) {
		for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacao
					&& nomeRestricao.equalsIgnoreCase(violacao.getConstraintName())) {
				return true;
			}
		}
		return false;
	}
}
