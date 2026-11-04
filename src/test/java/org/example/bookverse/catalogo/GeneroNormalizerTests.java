package org.example.bookverse.catalogo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes puros (sem Spring e sem banco) da conversão de categorias externas para gêneros do BookVerse.
 */
class GeneroNormalizerTests {

	@Test
	void deveConverterCategoriaConhecida() {
		assertEquals("Fantasia", GeneroNormalizer.normalizar(List.of("Fiction / Fantasy / Epic")));
	}

	@Test
	void devePriorizarTermoMaisEspecifico() {
		assertEquals("Ficção Científica", GeneroNormalizer.normalizar(List.of("Fiction", "Science Fiction")));
	}

	@Test
	void deveUsarGeneroPadraoQuandoNaoReconhece() {
		assertEquals(GeneroNormalizer.GENERO_PADRAO, GeneroNormalizer.normalizar(List.of("Dragons", "Wizards")));
		assertEquals(GeneroNormalizer.GENERO_PADRAO, GeneroNormalizer.normalizar(null));
	}
}
