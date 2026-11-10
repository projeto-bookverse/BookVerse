package org.example.bookverse.dto;

/**
 * Dados que o usuário pode alterar no próprio perfil. E-mail, senha e tipo de conta não são editáveis por aqui.
 */
public record UsuarioAtualizacaoRequest(String nome, String fotoPerfil, String bio) {
}
