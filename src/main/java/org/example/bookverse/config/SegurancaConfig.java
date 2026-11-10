package org.example.bookverse.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regras de acesso da API e configuração dos tokens JWT.
 * Somente o cadastro e o login funcionam sem token; cadastrar e editar livros exige conta de administrador;
 * todo o resto exige usuário autenticado. Quem é o dono de cada dado é conferido nos services.
 */
@Configuration
@EnableWebSecurity
public class SegurancaConfig {

	/** Nome do campo do token que guarda o tipo de conta (LEITOR ou ADMIN). */
	public static final String CLAIM_TIPO_CONTA = "tipoConta";

	private static final int TAMANHO_MINIMO_SEGREDO = 32;

	@Bean
	public SecurityFilterChain filtroDeSeguranca(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
		AuthenticationEntryPoint naoAutenticado = naoAutenticado(jsonMapper);
		http
				// API sem sessão e sem formulários: cada requisição se identifica pelo token, então CSRF não se aplica
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(regras -> regras
						.requestMatchers(HttpMethod.POST, "/usuarios", "/autenticacao/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/livros").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/livros/**").hasRole("ADMIN")
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(servidor -> servidor
						.jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDePermissoes()))
						.authenticationEntryPoint(naoAutenticado))
				.exceptionHandling(erros -> erros
						.authenticationEntryPoint(naoAutenticado)
						.accessDeniedHandler(acessoNegado(jsonMapper)));
		return http.build();
	}

	@Bean
	public JwtEncoder jwtEncoder(@Value("${bookverse.jwt.segredo}") String segredo) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(chave(segredo)));
	}

	@Bean
	public JwtDecoder jwtDecoder(@Value("${bookverse.jwt.segredo}") String segredo) {
		return NimbusJwtDecoder.withSecretKey(chave(segredo)).macAlgorithm(MacAlgorithm.HS256).build();
	}

	/** Transforma o tipo de conta do token (LEITOR/ADMIN) nas permissões ROLE_LEITOR/ROLE_ADMIN. */
	private static JwtAuthenticationConverter conversorDePermissoes() {
		JwtGrantedAuthoritiesConverter permissoes = new JwtGrantedAuthoritiesConverter();
		permissoes.setAuthoritiesClaimName(CLAIM_TIPO_CONTA);
		permissoes.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
		conversor.setJwtGrantedAuthoritiesConverter(permissoes);
		return conversor;
	}

	private static SecretKey chave(String segredo) {
		if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO) {
			throw new IllegalStateException("A variável de ambiente JWT_SECRET deve ter pelo menos "
					+ TAMANHO_MINIMO_SEGREDO + " caracteres.");
		}
		return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	/** 401: requisição sem token, com token inválido ou expirado. */
	private static AuthenticationEntryPoint naoAutenticado(JsonMapper jsonMapper) {
		return (request, response, exception) -> escreverErro(jsonMapper, request, response, HttpStatus.UNAUTHORIZED,
				"É necessário entrar no sistema para acessar este recurso.");
	}

	/** 403: usuário autenticado, mas sem a permissão exigida pela rota (ex.: leitor cadastrando livro). */
	private static AccessDeniedHandler acessoNegado(JsonMapper jsonMapper) {
		return (request, response, exception) -> escreverErro(jsonMapper, request, response, HttpStatus.FORBIDDEN,
				"Você não tem permissão para realizar esta ação.");
	}

	/** Responde no mesmo formato Problem Details do TratadorGlobalDeErros. */
	private static void escreverErro(JsonMapper jsonMapper, HttpServletRequest request,
			HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
		// O Jackson monta o JSON, então caracteres especiais na URI (ex.: aspas) são escapados corretamente
		Map<String, Object> corpo = new LinkedHashMap<>();
		corpo.put("type", "about:blank");
		corpo.put("title", status.getReasonPhrase());
		corpo.put("status", status.value());
		corpo.put("detail", mensagem);
		corpo.put("instance", request.getRequestURI());

		response.setStatus(status.value());
		response.setContentType("application/problem+json");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(jsonMapper.writeValueAsString(corpo));
	}
}
