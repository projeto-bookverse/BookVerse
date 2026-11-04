package org.example.bookverse.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SenhaHashServiceTests {

	private final SenhaHashService senhaHashService = new SenhaHashService();

	@Test
	void deveGerarHashNoFormatoEsperado() {
		String hash = senhaHashService.hash("senha-de-teste-segura");

		String[] partes = hash.split("\\$");
		assertEquals(4, partes.length);
		assertEquals("pbkdf2-sha256", partes[0]);
		assertEquals("600000", partes[1]);
		assertFalse(hash.contains("senha-de-teste-segura"));
	}

	@Test
	void deveGerarHashesDiferentesParaAMesmaSenha() {
		// Cada hash usa um sal aleatório
		assertNotEquals(senhaHashService.hash("mesma-senha"), senhaHashService.hash("mesma-senha"));
	}
}
