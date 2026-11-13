package org.example.bookverse.config;

import org.example.bookverse.dto.TokenResponse;
import org.example.bookverse.dto.UsuarioLogadoResponse;
import org.example.bookverse.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Gera o token entregue no login. O token carrega o ID do usuário (subject) e o tipo de conta,
 * é assinado com a chave JWT_SECRET e vale pelo tempo definido em bookverse.jwt.validade.
 * A validação dos tokens recebidos é feita pelo Spring Security (ver SegurancaConfig).
 */
@Service
public class JwtService {

	private static final String EMISSOR = "bookverse";

	private final JwtEncoder jwtEncoder;
	private final Duration validade;

	public JwtService(JwtEncoder jwtEncoder, @Value("${bookverse.jwt.validade}") Duration validade) {
		this.jwtEncoder = jwtEncoder;
		this.validade = validade;
	}

	public TokenResponse gerarToken(Usuario usuario) {
		Instant agora = Instant.now();
		Instant expiraEm = agora.plus(validade);

		JwtClaimsSet dados = JwtClaimsSet.builder()
				.issuer(EMISSOR)
				.issuedAt(agora)
				.expiresAt(expiraEm)
				.subject(usuario.getId().toString())
				.claim(SegurancaConfig.CLAIM_TIPO_CONTA, usuario.getTipoConta().name())
				.build();
		JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, dados)).getTokenValue();

		return new TokenResponse(token, "Bearer", OffsetDateTime.ofInstant(expiraEm, ZoneId.systemDefault()),
				UsuarioLogadoResponse.from(usuario));
	}

	/** ID do usuário que fez a requisição, lido do token já validado. */
	public static UUID idDoUsuario(Jwt token) {
		return UUID.fromString(token.getSubject());
	}
}
