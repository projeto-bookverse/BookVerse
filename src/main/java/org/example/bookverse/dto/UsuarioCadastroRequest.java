package org.example.bookverse.dto;

import java.time.LocalDate;

public record UsuarioCadastroRequest(
		String nome,
		String email,
		String senha,
		LocalDate dataNascimento,
		String fotoPerfil,
		String bio) {
}
