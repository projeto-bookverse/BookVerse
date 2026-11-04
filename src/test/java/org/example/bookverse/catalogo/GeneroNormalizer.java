package org.example.bookverse.catalogo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Converte as categorias das APIs externas (em inglês e sem padrão, ex.: "Fiction / Fantasy / Epic")
 * para os gêneros usados no BookVerse. A coluna livros.genero é obrigatória, então sempre há um valor padrão.
 */
public final class GeneroNormalizer {

	public static final String GENERO_PADRAO = "Outros";

	/** Ordem importa: o primeiro termo encontrado define o gênero ("science fiction" antes de "fiction"). */
	private static final Map<String, String> MAPEAMENTO = new LinkedHashMap<>();

	static {
		MAPEAMENTO.put("science fiction", "Ficção Científica");
		MAPEAMENTO.put("fantasy", "Fantasia");
		MAPEAMENTO.put("horror", "Terror");
		MAPEAMENTO.put("mystery", "Mistério");
		MAPEAMENTO.put("detective", "Mistério");
		MAPEAMENTO.put("thriller", "Suspense");
		MAPEAMENTO.put("suspense", "Suspense");
		MAPEAMENTO.put("romance", "Romance");
		MAPEAMENTO.put("love stories", "Romance");
		MAPEAMENTO.put("biography", "Biografia");
		MAPEAMENTO.put("history", "História");
		MAPEAMENTO.put("juvenile", "Infantil");
		MAPEAMENTO.put("children", "Infantil");
		MAPEAMENTO.put("poetry", "Poesia");
		MAPEAMENTO.put("fiction", "Ficção");
	}

	private GeneroNormalizer() {
	}

	public static String normalizar(List<String> categorias) {
		if (categorias == null) {
			return GENERO_PADRAO;
		}
		for (Map.Entry<String, String> entrada : MAPEAMENTO.entrySet()) {
			for (String categoria : categorias) {
				if (categoria != null && categoria.toLowerCase(Locale.ROOT).contains(entrada.getKey())) {
					return entrada.getValue();
				}
			}
		}
		return GENERO_PADRAO;
	}
}
