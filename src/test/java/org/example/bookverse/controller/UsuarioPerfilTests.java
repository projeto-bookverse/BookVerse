package org.example.bookverse.controller;

import org.example.bookverse.config.JwtService;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.repository.UsuarioRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa a consulta (RF02) e a edição (RF03) de perfil de ponta a ponta.
 * Cada teste roda em uma transação desfeita ao final, então nada fica salvo no banco.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioPerfilTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private JwtService jwtService;

	private Usuario leitora;

	private String tokenDaLeitora;

	@BeforeEach
	void configurar() {
		leitora = novoUsuario("Leitora Teste");
		tokenDaLeitora = tokenDe(leitora);
	}

	@Test
	void deveConsultarPerfilSemExporSenha() throws Exception {
		Usuario outroLeitor = novoUsuario("Outro Leitor");

		mockMvc.perform(get("/usuarios/{id}", outroLeitor.getId()).header(HttpHeaders.AUTHORIZATION, tokenDaLeitora))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Outro Leitor"))
				.andExpect(jsonPath("$.tipoConta").value("LEITOR"))
				.andExpect(jsonPath("$.senha").doesNotExist())
				.andExpect(jsonPath("$.senhaHash").doesNotExist());
	}

	@Test
	void naoDeveExporEmailENascimentoNoPerfilDeOutroUsuario() throws Exception {
		Usuario outroLeitor = novoUsuario("Outro Leitor");

		mockMvc.perform(get("/usuarios/{id}", outroLeitor.getId()).header(HttpHeaders.AUTHORIZATION, tokenDaLeitora))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").doesNotExist())
				.andExpect(jsonPath("$.dataNascimento").doesNotExist());
	}

	@Test
	void deveMostrarEmailENascimentoNoProprioPerfil() throws Exception {
		mockMvc.perform(get("/usuarios/{id}", leitora.getId()).header(HttpHeaders.AUTHORIZATION, tokenDaLeitora))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(leitora.getEmail()))
				.andExpect(jsonPath("$.dataNascimento").value("2000-01-15"));
	}

	@Test
	void deveResponderNaoEncontradoParaPerfilInexistente() throws Exception {
		mockMvc.perform(get("/usuarios/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, tokenDaLeitora))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Usuário não encontrado."));
	}

	@Test
	void deveEditarOProprioPerfil() throws Exception {
		editar(leitora.getId(), tokenDaLeitora, """
				{"nome": "  Leitora Renomeada  ", "fotoPerfil": "https://example.com/foto.jpg", "bio": "Amo clássicos."}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Leitora Renomeada"))
				.andExpect(jsonPath("$.fotoPerfil").value("https://example.com/foto.jpg"))
				.andExpect(jsonPath("$.bio").value("Amo clássicos."))
				.andExpect(jsonPath("$.email").value(leitora.getEmail()));
	}

	@Test
	void naoDeveEditarPerfilDeOutroUsuario() throws Exception {
		Usuario outroLeitor = novoUsuario("Outro Leitor");

		editar(outroLeitor.getId(), tokenDaLeitora, """
				{"nome": "Nome Alterado"}
				""")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Você só pode editar o próprio perfil."));
	}

	@Test
	void naoDeveEditarPerfilSemNome() throws Exception {
		editar(leitora.getId(), tokenDaLeitora, """
				{"nome": " ", "bio": "Sem nome"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O nome é obrigatório."));
	}

	@Test
	void naoDeveEditarPerfilSemEstarLogado() throws Exception {
		mockMvc.perform(put("/usuarios/{id}", leitora.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome": "Sem Token"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions editar(UUID id, String token, String corpo) throws Exception {
		return mockMvc.perform(put("/usuarios/{id}", id)
				.header(HttpHeaders.AUTHORIZATION, token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(corpo));
	}

	private Usuario novoUsuario(String nome) {
		String email = "teste-" + UUID.randomUUID().toString().substring(0, 8) + "@bookverse.com";
		return usuarioRepository.save(new Usuario(nome, email, "hash-ficticio", LocalDate.of(2000, 1, 15),
				TipoConta.LEITOR));
	}

	private String tokenDe(Usuario usuario) {
		return "Bearer " + jwtService.gerarToken(usuario).token();
	}
}
