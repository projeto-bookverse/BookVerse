package org.example.bookverse.controller;

import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa as avaliações de ponta a ponta (HTTP → Service → PostgreSQL local), garantindo que as entidades,
 * o banco e o tratamento de erros funcionam juntos. Cada teste sofre rollback ao final.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AvaliacaoApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private LivroRepository livroRepository;

	private Usuario leitora;

	private Livro livro;

	@BeforeEach
	void configurar() {
		String sufixo = UUID.randomUUID().toString().substring(0, 8);
		leitora = usuarioRepository.save(new Usuario("Leitora Teste", "teste-" + sufixo + "@bookverse.com",
				"hash-ficticio", LocalDate.of(2000, 1, 15), TipoConta.LEITOR));
		livro = livroRepository.save(new Livro("Vidas Secas " + sufixo, "Graciliano Ramos", "Romance"));
	}

	@Test
	void deveAvaliarSoComEstrelasEListarPorLivro() throws Exception {
		avaliar(leitora.getId(), livro.getId(), 5, null)
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.nota").value(5))
				.andExpect(jsonPath("$.nomeUsuario").value("Leitora Teste"))
				.andExpect(jsonPath("$.textoResenha").doesNotExist())
				.andExpect(jsonPath("$.flagSpoiler").value(false))
				.andExpect(jsonPath("$.criadoEm").isNotEmpty());

		mockMvc.perform(get("/livros/{id}/avaliacoes", livro.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void naoDeveAvaliarOMesmoLivroDuasVezes() throws Exception {
		avaliar(leitora.getId(), livro.getId(), 4, "Gostei").andExpect(status().isCreated());

		avaliar(leitora.getId(), livro.getId(), 2, "Mudei de ideia")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Este usuário já avaliou este livro."));
	}

	@Test
	void deveRejeitarNotaForaDoIntervaloComMensagemEmPortugues() throws Exception {
		avaliar(leitora.getId(), livro.getId(), 6, "Resenha")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A nota deve ser entre 1 e 5."));
	}

	@Test
	void deveResponderNaoEncontradoParaLivroInexistente() throws Exception {
		avaliar(leitora.getId(), UUID.randomUUID(), 4, "Resenha")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Livro não encontrado."));
	}

	@Test
	void deveEditarEExcluirApenasAPropriaAvaliacao() throws Exception {
		String id = idDaResposta(avaliar(leitora.getId(), livro.getId(), 3, "Mediano"));

		mockMvc.perform(put("/avaliacoes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId": "%s", "nota": 4, "textoResenha": "Melhorou", "flagSpoiler": false}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Somente o autor da avaliação pode alterá-la ou excluí-la."));

		mockMvc.perform(put("/avaliacoes/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"usuarioId": "%s", "nota": 4, "textoResenha": "Melhorou", "flagSpoiler": true}
								""".formatted(leitora.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nota").value(4))
				.andExpect(jsonPath("$.flagSpoiler").value(true));

		mockMvc.perform(delete("/avaliacoes/{id}", id).param("usuarioId", leitora.getId().toString()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/avaliacoes/{id}", id))
				.andExpect(status().isNotFound());
	}

	private ResultActions avaliar(UUID usuarioId, UUID livroId, int nota, String resenha) throws Exception {
		String resenhaJson = resenha == null ? "null" : "\"" + resenha + "\"";
		return mockMvc.perform(post("/avaliacoes")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"usuarioId": "%s", "livroId": "%s", "nota": %d, "textoResenha": %s, "flagSpoiler": false}
						""".formatted(usuarioId, livroId, nota, resenhaJson)));
	}

	private String idDaResposta(ResultActions resultado) throws Exception {
		String corpo = resultado.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return corpo.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");
	}
}
