package org.example.bookverse.controller;

import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o cadastro de usuários de ponta a ponta (HTTP → Service → PostgreSQL local).
 * Cada teste roda em uma transação desfeita ao final, então nada fica salvo no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioControllerTests {

	private static final String SENHA = "senha-de-teste-segura";

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private UsuarioRepository usuarioRepository;

	@Test
	void deveCadastrarUsuario() throws Exception {
		String email = emailAleatorio();

		cadastrar(json("Leitora Teste", email, SENHA, "2000-01-15"))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.tipoConta").value("LEITOR"))
				.andExpect(jsonPath("$.dataCadastro").isNotEmpty())
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void deveGravarSenhaComoHash() throws Exception {
		String email = emailAleatorio();
		cadastrar(json("Leitora Teste", email, SENHA, "2000-01-15")).andExpect(status().isCreated());

		Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
		assertNotEquals(SENHA, usuario.getSenhaHash());
		assertTrue(usuario.getSenhaHash().startsWith("pbkdf2-sha256$"));
	}

	@Test
	void deveGravarEmailEmMinusculasESemEspacos() throws Exception {
		String email = emailAleatorio();
		cadastrar(json("Leitora Teste", "  " + email.toUpperCase() + "  ", SENHA, "2000-01-15"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void naoDeveCadastrarEmailRepetido() throws Exception {
		String email = emailAleatorio();
		cadastrar(json("Leitora Teste", email, SENHA, "2000-01-15")).andExpect(status().isCreated());

		cadastrar(json("Outra Pessoa", email.toUpperCase(), SENHA, "1990-05-20"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um usuário cadastrado com o e-mail: " + email));
	}

	@Test
	void deveResponderConflitoQuandoBancoBarraEmailRepetido() throws Exception {
		// Simula dois cadastros simultâneos: a checagem prévia não vê o primeiro,
		// e quem barra o segundo é a restrição única do banco.
		String email = emailAleatorio();
		usuarioRepository.saveAndFlush(new Usuario("Primeira", email, "hash", LocalDate.of(2000, 1, 15), TipoConta.LEITOR));
		doReturn(Optional.empty()).when(usuarioRepository).findByEmail(anyString());

		cadastrar(json("Segunda", email, SENHA, "2000-01-15"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já existe um usuário cadastrado com o e-mail: " + email));
	}

	@Test
	void naoDeveCadastrarSemCampoObrigatorio() throws Exception {
		cadastrar("""
				{"email": "%s", "senha": "%s", "dataNascimento": "2000-01-15"}
				""".formatted(emailAleatorio(), SENHA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O nome é obrigatório."));
	}

	@Test
	void naoDeveCadastrarNomeMaiorQueOPermitido() throws Exception {
		cadastrar(json("a".repeat(151), emailAleatorio(), SENHA, "2000-01-15"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O nome deve ter no máximo 150 caracteres."));
	}

	@Test
	void naoDeveCadastrarEmailInvalido() throws Exception {
		cadastrar(json("Leitora Teste", "email-sem-arroba", SENHA, "2000-01-15"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O e-mail informado não é válido."));
	}

	@Test
	void naoDeveCadastrarSenhaCurta() throws Exception {
		cadastrar(json("Leitora Teste", emailAleatorio(), "1234567", "2000-01-15"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A senha deve ter pelo menos 8 caracteres."));
	}

	@Test
	void naoDeveCadastrarDataDeNascimentoNoFuturo() throws Exception {
		cadastrar(json("Leitora Teste", emailAleatorio(), SENHA, LocalDate.now().plusDays(1).toString()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A data de nascimento não pode estar no futuro."));
	}

	@Test
	void deveResponderErroPadronizadoParaJsonMalFormado() throws Exception {
		cadastrar("{\"nome\": \"Leitora Teste\",")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.detail").isNotEmpty());
	}

	@Test
	void naoDeveFicarComUsuarioGravadoQuandoDadosSaoInvalidos() throws Exception {
		String email = emailAleatorio();
		cadastrar(json("Leitora Teste", email, "curta", "2000-01-15")).andExpect(status().isBadRequest());

		assertEquals(Optional.empty(), usuarioRepository.findByEmail(email));
	}

	private ResultActions cadastrar(String corpo) throws Exception {
		return mockMvc.perform(post("/usuarios")
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

	private static String json(String nome, String email, String senha, String dataNascimento) {
		return """
				{"nome": "%s", "email": "%s", "senha": "%s", "dataNascimento": "%s"}
				""".formatted(nome, email, senha, dataNascimento);
	}

	private static String emailAleatorio() {
		return "teste-" + UUID.randomUUID().toString().substring(0, 8) + "@bookverse.com";
	}
}
