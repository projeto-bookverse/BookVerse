package org.example.bookverse.dto;

import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;

import java.util.UUID;

/**
 * Dados básicos de quem acabou de entrar, devolvidos junto com o token. Permitem ao front-end mostrar o nome,
 * montar as rotas com o ID (perfil, estante) e saber se a conta é de administrador, sem uma chamada extra.
 */
public record UsuarioLogadoResponse(UUID id, String nome, String email, TipoConta tipoConta) {

	public static UsuarioLogadoResponse from(Usuario usuario) {
		return new UsuarioLogadoResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTipoConta());
	}
}
