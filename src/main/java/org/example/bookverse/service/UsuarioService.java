package org.example.bookverse.service;

import org.example.bookverse.dto.UsuarioCadastroRequest;
import org.example.bookverse.dto.UsuarioResponse;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.exception.EmailJaCadastradoException;
import org.example.bookverse.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final SenhaHashService senhaHashService;

	public UsuarioService(UsuarioRepository usuarioRepository, SenhaHashService senhaHashService) {
		this.usuarioRepository = usuarioRepository;
		this.senhaHashService = senhaHashService;
	}

	@Transactional
	public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
		validar(request);

		String email = request.email().trim();
		if (usuarioRepository.findByEmail(email).isPresent()) {
			throw new EmailJaCadastradoException(email);
		}

		Usuario usuario = new Usuario(
				request.nome().trim(),
				email,
				senhaHashService.hash(request.senha()),
				request.dataNascimento(),
				TipoConta.LEITOR);
		usuario.setFotoPerfil(request.fotoPerfil());
		usuario.setBio(request.bio());

		return UsuarioResponse.from(usuarioRepository.saveAndFlush(usuario));
	}

	@Transactional(readOnly = true)
	public boolean emailCadastrado(String email) {
		return usuarioRepository.findByEmail(email.trim()).isPresent();
	}

	private void validar(UsuarioCadastroRequest request) {
		if (request == null
				|| request.nome() == null || request.nome().isBlank()
				|| request.email() == null || request.email().isBlank()
				|| request.senha() == null || request.senha().isBlank()
				|| request.dataNascimento() == null) {
			throw new IllegalArgumentException(
					"Nome, e-mail, senha e data de nascimento são obrigatórios.");
		}
	}
}
