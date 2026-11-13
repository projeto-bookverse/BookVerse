package org.example.bookverse.config;

import org.example.bookverse.controller.LivroController;
import org.example.bookverse.dto.LivroRequest;
import org.example.bookverse.dto.LivroResponse;
import org.example.bookverse.dto.PaginaResponse;
import org.example.bookverse.service.LivroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Simula o navegador chamando a API a partir do front-end Web (CORS, RNF09). Usa as regras de segurança reais
 * e o service de livros simulado, então não depende do banco. Origem permitida por padrão: localhost:8000.
 */
@WebMvcTest(LivroController.class)
@Import(SegurancaConfig.class)
class CorsTests {

	private static final String FRONT_END = "http://localhost:8000";
	private static final String OUTRO_SITE = "http://localhost:5173";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LivroService livroService;

	@Test
	void deveAutorizarPerguntaPreviaDoFrontEnd() throws Exception {
		// O navegador pergunta antes de enviar o token; a pergunta não leva token e mesmo assim deve passar
		mockMvc.perform(options("/livros")
						.header(HttpHeaders.ORIGIN, FRONT_END)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_END))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
	}

	@Test
	void deveAutorizarPerguntaPreviaDoLoginComJson() throws Exception {
		mockMvc.perform(options("/autenticacao/login")
						.header(HttpHeaders.ORIGIN, FRONT_END)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_END));
	}

	@Test
	void deveAutorizarFrontEndAbertoPorEnderecoNumerico() throws Exception {
		// Para o navegador, localhost e 127.0.0.1 são origens diferentes
		mockMvc.perform(options("/livros")
						.header(HttpHeaders.ORIGIN, "http://127.0.0.1:8000")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:8000"));
	}

	@Test
	void naoDeveAutorizarOutroEndereco() throws Exception {
		mockMvc.perform(options("/livros")
						.header(HttpHeaders.ORIGIN, OUTRO_SITE)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void naoDeveAutorizarMetodoForaDaLista() throws Exception {
		mockMvc.perform(options("/livros")
						.header(HttpHeaders.ORIGIN, FRONT_END)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
				.andExpect(status().isForbidden());
	}

	@Test
	void deveLiberarRespostaEOEnderecoDoRecursoCriadoParaOFrontEnd() throws Exception {
		UUID id = UUID.randomUUID();
		when(livroService.cadastrar(any(LivroRequest.class))).thenReturn(livro(id));

		mockMvc.perform(post("/livros").with(usuarioCom("ROLE_ADMIN"))
						.header(HttpHeaders.ORIGIN, FRONT_END)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"titulo":"Vidas Secas","autor":"Graciliano Ramos","genero":"Romance"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT_END))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")))
				.andExpect(header().string(HttpHeaders.LOCATION, "/livros/" + id));
	}

	@Test
	void naoDeveResponderChamadaDeOutroEndereco() throws Exception {
		mockMvc.perform(get("/livros").with(usuarioCom("ROLE_LEITOR"))
						.header(HttpHeaders.ORIGIN, OUTRO_SITE))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void chamadaSemOrigemContinuaFuncionando() throws Exception {
		// Postman, IntelliJ e o app Flutter não enviam Origin e não são afetados pelo CORS
		when(livroService.listar(anyInt(), anyInt()))
				.thenReturn(new PaginaResponse<>(List.of(livro(UUID.randomUUID())), 0, 20, 1, 1));

		mockMvc.perform(get("/livros").with(usuarioCom("ROLE_LEITOR")))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	private static RequestPostProcessor usuarioCom(String permissao) {
		return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
				.authorities(new SimpleGrantedAuthority(permissao));
	}

	private static LivroResponse livro(UUID id) {
		return new LivroResponse(id, "Vidas Secas", "Graciliano Ramos", null, null, null, "Romance",
				1938, 176, 0.0, 0);
	}
}
