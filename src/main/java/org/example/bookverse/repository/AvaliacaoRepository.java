package org.example.bookverse.repository;

import org.example.bookverse.entity.Avaliacao;
import org.example.bookverse.repository.projection.ResumoAvaliacoesLivro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {

	List<Avaliacao> findByLivroId(UUID livroId);

	List<Avaliacao> findByLivroIdOrderByCriadoEmDesc(UUID livroId);

	List<Avaliacao> findByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

	boolean existsByUsuarioIdAndLivroId(UUID usuarioId, UUID livroId);

	@Query("""
			select a.livro.id as livroId, avg(a.nota) as mediaNotas, count(a) as totalAvaliacoes
			from Avaliacao a
			where a.livro.id in :livroIds
			group by a.livro.id
			""")
	List<ResumoAvaliacoesLivro> resumirPorLivroIds(@Param("livroIds") List<UUID> livroIds);

	@Query("select avg(a.nota) from Avaliacao a where a.livro.id = :livroId")
	Double calcularMediaNotasPorLivroId(@Param("livroId") UUID livroId);

	long countByLivroId(UUID livroId);
}
