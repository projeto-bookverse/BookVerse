package org.example.bookverse.dto;

import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioResponse(
		UUID id,
		String nome,
		String email,
		LocalDate dataNascimento,
		LocalDateTime dataCadastro,
		String fotoPerfil,
		String bio,
		TipoConta tipoConta) {

	/** Perfil completo, para o próprio usuário. */
	public static UsuarioResponse from(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				usuario.getEmail(),
				usuario.getDataNascimento(),
				usuario.getDataCadastro(),
				usuario.getFotoPerfil(),
				usuario.getBio(),
				usuario.getTipoConta());
	}

	/** Perfil visto por outra pessoa: sem e-mail e sem data de nascimento, que são dados pessoais. */
	public static UsuarioResponse publico(Usuario usuario) {
		return new UsuarioResponse(
				usuario.getId(),
				usuario.getNome(),
				null,
				null,
				usuario.getDataCadastro(),
				usuario.getFotoPerfil(),
				usuario.getBio(),
				usuario.getTipoConta());
	}
}
