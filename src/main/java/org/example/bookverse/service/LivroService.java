
package org.example.bookverse.service;

import org.example.bookverse.dto.LivroRequest;
import org.example.bookverse.dto.LivroResponse;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LivroService {

    private final LivroRepository livroRepository;

    public LivroService(LivroRepository livroRepository) {
        this.livroRepository = livroRepository;
    }

    @Transactional
    public LivroResponse cadastrar(LivroRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados do livro são obrigatórios."
            );
        }

        if (request.titulo() == null || request.titulo().isBlank()) {
            throw new IllegalArgumentException(
                    "O título é obrigatório."
            );
        }

        if (request.autor() == null || request.autor().isBlank()) {
            throw new IllegalArgumentException(
                    "O autor é obrigatório."
            );
        }

        if (request.genero() == null || request.genero().isBlank()) {
            throw new IllegalArgumentException(
                    "O gênero é obrigatório."
            );
        }

        if (request.anoPublicacao() != null
                && request.anoPublicacao() <= 0) {
            throw new IllegalArgumentException(
                    "O ano de publicação deve ser positivo."
            );
        }

        if (request.numPaginas() != null
                && request.numPaginas() <= 0) {
            throw new IllegalArgumentException(
                    "O número de páginas deve ser positivo."
            );
        }

        String isbn = request.isbn();

        if (isbn != null && isbn.isBlank()) {
            isbn = null;
        } else if (isbn != null) {
            isbn = isbn.trim();
        }

        if (isbn != null && livroRepository.findByIsbn(isbn).isPresent()) {
            throw new IllegalStateException(
                    "Já existe um livro cadastrado com esse ISBN."
            );
        }

        Livro livro = new Livro(
                request.titulo().trim(),
                request.autor().trim(),
                request.genero().trim()
        );

        livro.setIsbn(isbn);
        livro.setCapaUrl(request.capaUrl());
        livro.setSinopse(request.sinopse());
        livro.setAnoPublicacao(request.anoPublicacao());
        livro.setNumPaginas(request.numPaginas());

        Livro livroSalvo = livroRepository.save(livro);

        return LivroResponse.from(livroSalvo);
    }
}
