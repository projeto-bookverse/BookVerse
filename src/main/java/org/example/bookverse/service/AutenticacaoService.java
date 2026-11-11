package org.example.bookverse.service;

import org.example.bookverse.config.JwtService;
import org.example.bookverse.dto.LoginRequest;
import org.example.bookverse.dto.TokenResponse;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.exception.CredenciaisInvalidasException;
import org.example.bookverse.exception.DadosInvalidosException;
import org.example.bookverse.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AutenticacaoService {

	private final UsuarioRepository usuarioRepository;
	private final SenhaHashService senhaHashService;
	private final JwtService jwtService;
	private volatile String hashFicticio;

	public AutenticacaoService(UsuarioRepository usuarioRepository, SenhaHashService senhaHashService,
			JwtService jwtService) {
		this.usuarioRepository = usuarioRepository;
		this.senhaHashService = senhaHashService;
		this.jwtService = jwtService;
	}

	@Transactional(readOnly = true)
	public TokenResponse entrar(LoginRequest request) {
		if (request == null || estaVazio(request.email()) || estaVazio(request.senha())) {
			throw new DadosInvalidosException("Informe o e-mail e a senha.");
		}

		// O e-mail é gravado em minúsculas no cadastro, então o login também ignora maiúsculas
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

		// Mesmo sem usuário, a senha é conferida contra um hash fictício: assim a resposta demora o mesmo tempo
		// nos dois casos e não dá para descobrir, medindo o tempo, quais e-mails estão cadastrados.
		String hashArmazenado = usuario.map(Usuario::getSenhaHash).orElseGet(this::hashFicticio);
		boolean senhaCorreta = senhaHashService.verificar(request.senha(), hashArmazenado);
		if (usuario.isEmpty() || !senhaCorreta) {
			throw new CredenciaisInvalidasException();
		}
		return jwtService.gerarToken(usuario.get());
	}

	/** Gerado uma única vez, no primeiro login com e-mail inexistente, com o mesmo custo de um hash real. */
	private String hashFicticio() {
		String hash = hashFicticio;
		if (hash == null) {
			hash = senhaHashService.hash(UUID.randomUUID().toString());
			hashFicticio = hash;
		}
		return hash;
	}

	private static boolean estaVazio(String valor) {
		return valor == null || valor.isBlank();
	}
}
