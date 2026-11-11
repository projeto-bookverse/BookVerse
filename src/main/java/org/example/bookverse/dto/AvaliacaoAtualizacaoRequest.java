package org.example.bookverse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Só o autor pode editar; ele é identificado pelo token de acesso, por isso não há usuarioId aqui. */
public record AvaliacaoAtualizacaoRequest(
		@NotNull(message = "A nota é obrigatória.")
		@Min(value = 1, message = "A nota deve ser entre 1 e 5.")
		@Max(value = 5, message = "A nota deve ser entre 1 e 5.")
		Short nota,
		@Size(max = 5000, message = "A resenha deve ter no máximo 5000 caracteres.")
		String textoResenha,
		boolean flagSpoiler) {
}
