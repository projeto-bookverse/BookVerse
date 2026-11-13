package org.example.bookverse.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Página de resultados no formato fixo da API. Usado no lugar do Page do Spring, cujo JSON traz campos
 * internos e pode mudar entre versões.
 *
 * @param conteudo     itens da página
 * @param pagina       número da página, a partir de 0
 * @param tamanho      quantidade máxima de itens por página
 * @param totalItens   total de itens em todas as páginas
 * @param totalPaginas total de páginas
 */
public record PaginaResponse<T>(
		List<T> conteudo,
		int pagina,
		int tamanho,
		long totalItens,
		int totalPaginas) {

	public static <T> PaginaResponse<T> from(Page<T> page) {
		return new PaginaResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}
}
