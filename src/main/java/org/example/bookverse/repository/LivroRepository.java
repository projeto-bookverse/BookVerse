package org.example.bookverse.repository;

import org.example.bookverse.entity.Livro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LivroRepository extends JpaRepository<Livro, UUID> {

	Optional<Livro> findByIsbn(String isbn);
}
