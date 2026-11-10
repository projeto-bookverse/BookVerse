package org.example.bookverse.dto;

import java.time.OffsetDateTime;

/**
 * Resposta do login. O front-end envia o token nas próximas requisições,
 * no cabeçalho "Authorization: Bearer {token}", até a data de expiração (com fuso horário).
 */
public record TokenResponse(String token, String tipo, OffsetDateTime expiraEm) {
}
