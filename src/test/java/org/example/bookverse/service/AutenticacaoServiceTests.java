package org.example.bookverse.service;

import org.example.bookverse.config.JwtService;
import org.example.bookverse.dto.LoginRequest;
import org.example.bookverse.dto.TokenResponse;
import org.example.bookverse.dto.UsuarioLogadoResponse;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.exception.CredenciaisInvalidasException;
import org.example.bookverse.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacaoServiceTests {

	private UsuarioRepository usuarioRepository;
	private SenhaHashService senhaHashService;
	private JwtService jwtService;
	private AutenticacaoService autenticacaoService;

	@BeforeEach
	void configurar() {
		usuarioRepository = mock(UsuarioRepository.class);
		senhaHashService = mock(SenhaHashService.class);
		jwtService = mock(JwtService.class);
		autenticacaoService = new AutenticacaoService(usuarioRepository, senhaHashService, jwtService);
	}

	@Test
	void deveConferirSenhaMesmoQuandoEmailNaoExiste() {
		// Sem essa conferência, o login com e-mail inexistente responderia bem mais rápido e revelaria
		// quais e-mails estão cadastrados.
		when(usuarioRepository.findByEmail("ninguem@bookverse.com")).thenReturn(Optional.empty());
		when(senhaHashService.hash(anyString())).thenReturn("hash-ficticio");

		assertThrows(CredenciaisInvalidasException.class,
				() -> autenticacaoService.entrar(new LoginRequest("ninguem@bookverse.com", "qualquer-senha")));

		verify(senhaHashService).verificar("qualquer-senha", "hash-ficticio");
		verify(jwtService, never()).gerarToken(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void deveGerarHashFicticioUmaUnicaVez() {
		when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
		when(senhaHashService.hash(anyString())).thenReturn("hash-ficticio");

		for (int i = 0; i < 3; i++) {
			assertThrows(CredenciaisInvalidasException.class,
					() -> autenticacaoService.entrar(new LoginRequest("ninguem@bookverse.com", "senha")));
		}

		verify(senhaHashService, times(1)).hash(anyString());
	}

	@Test
	void deveEntrarComSenhaCorreta() {
		Usuario usuario = new Usuario("Leitora", "leitora@bookverse.com", "hash-real", LocalDate.of(2000, 1, 15),
				TipoConta.LEITOR);
		TokenResponse token = new TokenResponse("token", "Bearer", OffsetDateTime.now(),
				UsuarioLogadoResponse.from(usuario));
		when(usuarioRepository.findByEmail("leitora@bookverse.com")).thenReturn(Optional.of(usuario));
		when(senhaHashService.verificar("senha-certa", "hash-real")).thenReturn(true);
		when(jwtService.gerarToken(usuario)).thenReturn(token);

		assertSame(token, autenticacaoService.entrar(new LoginRequest("  Leitora@BookVerse.com ", "senha-certa")));
		verify(senhaHashService, never()).hash(anyString());
	}

	@Test
	void naoDeveEntrarComSenhaErrada() {
		Usuario usuario = new Usuario("Leitora", "leitora@bookverse.com", "hash-real", LocalDate.of(2000, 1, 15),
				TipoConta.LEITOR);
		when(usuarioRepository.findByEmail("leitora@bookverse.com")).thenReturn(Optional.of(usuario));
		when(senhaHashService.verificar(eq("senha-errada"), eq("hash-real"))).thenReturn(false);

		assertThrows(CredenciaisInvalidasException.class,
				() -> autenticacaoService.entrar(new LoginRequest("leitora@bookverse.com", "senha-errada")));
		verify(jwtService, never()).gerarToken(usuario);
	}
}
