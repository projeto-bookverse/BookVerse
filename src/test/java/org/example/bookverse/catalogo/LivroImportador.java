package org.example.bookverse.catalogo;

import org.example.bookverse.entity.Livro;
import org.example.bookverse.repository.LivroRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

import java.util.List;
import java.util.Optional;

/**
 * Protótipo do futuro LivroService: único ponto de entrada para gravar livros no banco.
 * Faz "upsert": se o livro já existe (mesmo ISBN, ou mesmo título + autor quando não há ISBN) ele é atualizado,
 * senão é criado. Assim, rodar a mesma importação várias vezes nunca duplica registros.
 */
public class LivroImportador {

	private final LivroRepository livroRepository;

	public LivroImportador(LivroRepository livroRepository) {
		this.livroRepository = livroRepository;
	}

	public Livro importar(LivroDTO dto) {
		if (isBlank(dto.titulo()) || isBlank(dto.autor())) {
			throw new IllegalArgumentException("Título e autor são obrigatórios");
		}
		String isbn = normalizarIsbn(dto.isbn());

		Livro livro = buscarExistente(dto, isbn).orElseGet(Livro::new);
		livro.setTitulo(dto.titulo().trim());
		livro.setAutor(dto.autor().trim());
		livro.setIsbn(isbn);
		livro.setCapaUrl(dto.capaUrl());
		livro.setSinopse(dto.sinopse());
		livro.setGenero(isBlank(dto.genero()) ? GeneroNormalizer.GENERO_PADRAO : dto.genero().trim());
		livro.setAnoPublicacao(dto.anoPublicacao());
		livro.setNumPaginas(dto.numPaginas());

		return livroRepository.save(livro);
	}

	public void importarTodos(List<LivroDTO> livros) {
		livros.forEach(this::importar);
	}

	private Optional<Livro> buscarExistente(LivroDTO dto, String isbn) {
		if (isbn != null) {
			return livroRepository.findByIsbn(isbn);
		}
		// Sem ISBN, compara título + autor ignorando maiúsculas (Query by Example, já incluso no JpaRepository).
		Livro exemplo = new Livro(dto.titulo().trim(), dto.autor().trim(), null);
		ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreCase();
		return livroRepository.findAll(Example.of(exemplo, matcher)).stream().findFirst();
	}

	/** Remove hífens e espaços: "978-85-359-1484-9" e "9788535914849" são o mesmo livro. */
	static String normalizarIsbn(String isbn) {
		if (isBlank(isbn)) {
			return null;
		}
		return isbn.replaceAll("[^0-9Xx]", "").toUpperCase();
	}

	private static boolean isBlank(String valor) {
		return valor == null || valor.isBlank();
	}
}
