package org.example.bookverse.catalogo;

import java.util.Optional;

/**
 * Fonte externa de dados de livros. O resto do sistema depende só desta interface,
 * então trocar ou adicionar uma API (Open Library, Google Books...) é criar uma nova implementação.
 * Implementações apenas buscam e convertem para {@link LivroDTO}; quem grava no banco é o {@link LivroImportador}.
 */
public interface CatalogoExterno {

	Optional<LivroDTO> buscarPorIsbn(String isbn);
}
