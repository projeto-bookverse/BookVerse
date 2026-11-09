package org.example.bookverse.dto;

import org.example.bookverse.entity.Livro;

import java.util.UUID;

public record RecomendacaoResponse(
        UUID id,
        String titulo,
        String autor,
        String capaUrl,
        String genero
) {
    public static RecomendacaoResponse from(Livro livro) {
        return new RecomendacaoResponse(
                livro.getId(),
                livro.getTitulo(),
                livro.getAutor(),
                livro.getCapaUrl(),
                livro.getGenero()
        );
    }
}