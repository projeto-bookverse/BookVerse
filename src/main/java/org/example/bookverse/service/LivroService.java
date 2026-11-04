package org.example.bookverse.service;

import org.example.bookverse.dto.LivroRequest;
import org.example.bookverse.dto.LivroResponse;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.exception.ConflitoException;
import org.example.bookverse.exception.DadosInvalidosException;
import org.example.bookverse.repository.LivroRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class LivroService {

	// Limites iguais aos das colunas da tabela livros
	private static final int TAMANHO_MAXIMO_TITULO = 255;
	private static final int TAMANHO_MAXIMO_AUTOR = 150;
	private static final int TAMANHO_MAXIMO_GENERO = 50;
	private static final int TAMANHO_MAXIMO_CAPA_URL = 255;

	private static final String RESTRICAO_ISBN_UNICO = "livros_isbn_key";
	private static final String MENSAGEM_ISBN_CADASTRADO = "Já existe um livro cadastrado com o ISBN: ";

	private final LivroRepository livroRepository;

	public LivroService(LivroRepository livroRepository) {
		this.livroRepository = livroRepository;
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
		livro.setIsbn(isbn);
		livro.setCapaUrl(textoOpcional(request.capaUrl()));
		livro.setSinopse(textoOpcional(request.sinopse()));
		livro.setAnoPublicacao(request.anoPublicacao());
		livro.setNumPaginas(request.numPaginas());

		try {
			return LivroResponse.from(livroRepository.saveAndFlush(livro));
		} catch (DataIntegrityViolationException exception) {
			// Dois cadastros simultâneos com o mesmo ISBN passam pela checagem acima;
			// o segundo é barrado pela restrição única do banco.
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
