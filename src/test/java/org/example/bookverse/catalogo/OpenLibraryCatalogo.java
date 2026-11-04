package org.example.bookverse.catalogo;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca livros na Open Library (https://openlibrary.org/dev/docs/api/books), que é gratuita e não exige chave.
 * Exemplo de chamada: GET /api/books?bibkeys=ISBN:9780547928227&format=json&jscmd=data
 * A resposta é um objeto cuja chave é "ISBN:<isbn>"; quando o livro não existe, volta "{}".
 */
public class OpenLibraryCatalogo implements CatalogoExterno {

	public static final String URL_BASE = "https://openlibrary.org";

	private static final Pattern ANO = Pattern.compile("\\d{4}");

	private final RestClient restClient;

	public OpenLibraryCatalogo(RestClient.Builder builder) {
		this.restClient = builder.baseUrl(URL_BASE).build();
	}

	@Override
	public Optional<LivroDTO> buscarPorIsbn(String isbn) {
		String isbnNormalizado = LivroImportador.normalizarIsbn(isbn);
		String chave = "ISBN:" + isbnNormalizado;

		Map<String, OpenLibraryLivro> resposta = restClient.get()
				.uri("/api/books?bibkeys={chave}&format=json&jscmd=data", chave)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});

		if (resposta == null || !resposta.containsKey(chave)) {
			return Optional.empty();
		}
		return Optional.of(converter(resposta.get(chave), isbnNormalizado));
	}

	private LivroDTO converter(OpenLibraryLivro livro, String isbn) {
		String autor = livro.authors() == null ? null
				: String.join(", ", livro.authors().stream().map(Nome::name).toList());
		List<String> assuntos = livro.subjects() == null ? List.of()
				: livro.subjects().stream().map(Nome::name).toList();

		return new LivroDTO(
				livro.title(),
				autor,
				isbn,
				livro.cover() == null ? null : livro.cover().large(),
				null, // a API de dados da Open Library não traz sinopse
				GeneroNormalizer.normalizar(assuntos),
				extrairAno(livro.publishDate()),
				livro.numberOfPages());
	}

	/** "publish_date" vem em formatos variados ("2012", "March 2012", "1937-09-21"); pega o primeiro ano. */
	private static Integer extrairAno(String data) {
		if (data == null) {
			return null;
		}
		Matcher matcher = ANO.matcher(data);
		return matcher.find() ? Integer.valueOf(matcher.group()) : null;
	}

	/** Espelha só os campos da resposta que o BookVerse usa; os demais são ignorados. */
	record OpenLibraryLivro(
			String title,
			List<Nome> authors,
			@JsonProperty("number_of_pages") Integer numberOfPages,
			@JsonProperty("publish_date") String publishDate,
			List<Nome> subjects,
			Capa cover) {
	}

	record Nome(String name) {
	}

	record Capa(String large) {
	}
}
