package org.example.bookverse.repository;

import org.example.bookverse.entity.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

	List<Avaliacao> findByLivroId(UUID livroId);
}
