package org.example.bookverse.repository.projection;

import java.util.UUID;

public interface ResumoAvaliacoesLivro {

	UUID getLivroId();

	Double getMediaNotas();

	Long getTotalAvaliacoes();
}
