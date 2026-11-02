
package org.example.bookverse.dto;
import org.example.bookverse.entity.Livro;
import java.util.UUID;

public record LivroResponse(
        UUID id,
        String titulo,
        String autor,
        String isbn,
        String capaUrl,
        String sinopse,
        String genero,
        Integer anoPublicacao,
        Integer numPaginas
) {
    public static LivroResponse from(Livro livro) {
        return new LivroResponse(
                livro.getId(),
                livro.getTitulo(),
                livro.getAutor(),
                livro.getIsbn(),
                livro.getCapaUrl(),
                livro.getSinopse(),
                livro.getGenero(),
                livro.getAnoPublicacao(),
                livro.getNumPaginas()
        );
    }
}
