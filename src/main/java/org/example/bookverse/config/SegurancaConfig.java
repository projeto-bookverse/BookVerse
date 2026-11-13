package org.example.bookverse.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
				// Usa a regra de CORS do bean abaixo; a pergunta prévia do navegador (OPTIONS) é respondida antes
				// da checagem de token, que ela nunca leva
				.cors(Customizer.withDefaults())
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

	/**
	 * CORS (RNF09): só os endereços do front-end configurados em bookverse.cors.origens podem chamar a API pelo
	 * navegador, com os métodos GET, POST, PUT e DELETE. O token vai no cabeçalho Authorization, não em cookie,
	 * então não é preciso liberar credenciais do navegador.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(@Value("${bookverse.cors.origens}") String origens) {
		CorsConfiguration regra = new CorsConfiguration();
		regra.setAllowedOrigins(Arrays.stream(origens.split(","))
				.map(String::trim)
				.filter(origem -> !origem.isEmpty())
				.toList());
		regra.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
		regra.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		// Deixa o front ler o endereço do recurso criado (respostas 201)
		regra.setExposedHeaders(List.of(HttpHeaders.LOCATION));
		// O navegador guarda a resposta da pergunta prévia por 1 hora
		regra.setMaxAge(Duration.ofHours(1));

		UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
		fonte.registerCorsConfiguration("/**", regra);
		return fonte;
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
