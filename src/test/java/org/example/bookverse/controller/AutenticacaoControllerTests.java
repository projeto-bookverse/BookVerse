package org.example.bookverse.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o login de ponta a ponta: cadastro → login → uso do token numa rota protegida.
 * Cada teste roda em uma transação desfeita ao final, então nada fica salvo no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutenticacaoControllerTests {

	private static final String SENHA = "senha-de-teste-segura";

	@Autowired
	private MockMvc mockMvc;

	private String email;

	private String usuarioId;

	@BeforeEach
	void cadastrarUsuario() throws Exception {
		email = "teste-" + UUID.randomUUID().toString().substring(0, 8) + "@bookverse.com";
		String corpo = mockMvc.perform(post("/usuarios")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome": "Leitora Teste", "email": "%s", "senha": "%s", "dataNascimento": "2000-01-15"}
								""".formatted(email, SENHA)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		usuarioId = corpo.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
	}

	@Test
	void deveEntrarEReceberToken() throws Exception {
		entrar(email, SENHA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.expiraEm").isNotEmpty())
				// A data de expiração traz o fuso horário (ex.: 2026-11-03T22:30:00-03:00)
				.andExpect(jsonPath("$.expiraEm").value(matchesPattern(".*([+-]\\d{2}:\\d{2}|Z)$")));
	}

	@Test
	void deveAcessarRotaProtegidaComOTokenRecebido() throws Exception {
		String token = tokenDoLogin(email, SENHA);

		mockMvc.perform(get("/usuarios/{id}", usuarioId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void deveEntrarIgnorandoMaiusculasNoEmail() throws Exception {
		entrar(email.toUpperCase(), SENHA).andExpect(status().isOk());
	}

	@Test
	void naoDeveEntrarComSenhaErrada() throws Exception {
		entrar(email, "senha-errada")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
	}

	@Test
	void naoDeveEntrarComEmailNaoCadastrado() throws Exception {
		// Mesma mensagem da senha errada, para não revelar quais e-mails estão cadastrados
		entrar("nao-cadastrado@bookverse.com", SENHA)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
	}

	@Test
	void naoDeveEntrarSemSenha() throws Exception {
		entrar(email, "")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Informe o e-mail e a senha."));
	}

	@Test
	void naoDeveAcessarRotaProtegidaSemToken() throws Exception {
		mockMvc.perform(get("/usuarios/{id}", usuarioId))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.detail").value("É necessário entrar no sistema para acessar este recurso."));
	}

	@Test
	void naoDeveAcessarRotaProtegidaComTokenInvalido() throws Exception {
		mockMvc.perform(get("/usuarios/{id}", usuarioId).header(HttpHeaders.AUTHORIZATION, "Bearer token-falso"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("É necessário entrar no sistema para acessar este recurso."));
	}

	private ResultActions entrar(String email, String senha) throws Exception {
		return mockMvc.perform(post("/autenticacao/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "senha": "%s"}
						""".formatted(email, senha)));
	}

	private String tokenDoLogin(String email, String senha) throws Exception {
		String corpo = entrar(email, senha).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return corpo.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
	}
}
