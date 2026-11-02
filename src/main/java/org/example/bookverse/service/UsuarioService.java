package org.example.bookverse.service;

import org.example.bookverse.dto.UsuarioCadastroRequest;
import org.example.bookverse.dto.UsuarioResponse;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.entity.enums.TipoConta;
import org.example.bookverse.exception.DadosInvalidosException;
import org.example.bookverse.exception.EmailJaCadastradoException;
import org.example.bookverse.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

	// Limites iguais aos das colunas da tabela usuarios
	private static final int TAMANHO_MAXIMO_NOME = 150;
	private static final int TAMANHO_MAXIMO_EMAIL = 150;
	private static final int TAMANHO_MAXIMO_FOTO_PERFIL = 255;
	private static final int TAMANHO_MINIMO_SENHA = 8;

	private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
	private static final String RESTRICAO_EMAIL_UNICO = "usuarios_email_key";

	private final UsuarioRepository usuarioRepository;
	private final SenhaHashService senhaHashService;

	public UsuarioService(UsuarioRepository usuarioRepository, SenhaHashService senhaHashService) {
		this.usuarioRepository = usuarioRepository;
		this.senhaHashService = senhaHashService;
	}

	@Transactional
	public UsuarioResponse cadastrar(UsuarioCadastroRequest request) {
		validar(request);

		String email = request.email().trim().toLowerCase(Locale.ROOT);
		if (usuarioRepository.findByEmail(email).isPresent()) {
			throw new EmailJaCadastradoException(email);
		}

		Usuario usuario = new Usuario(
				request.nome().trim(),
				email,
				senhaHashService.hash(request.senha()),
				request.dataNascimento(),
				TipoConta.LEITOR);
		usuario.setFotoPerfil(textoOpcional(request.fotoPerfil()));
		usuario.setBio(textoOpcional(request.bio()));

		try {
			return UsuarioResponse.from(usuarioRepository.saveAndFlush(usuario));
		} catch (DataIntegrityViolationException exception) {
			// Dois cadastros simultâneos com o mesmo e-mail passam pela checagem acima;
			// o segundo é barrado pela restrição única do banco.
			if (violouRestricao(exception, RESTRICAO_EMAIL_UNICO)) {
				throw new EmailJaCadastradoException(email);
			}
			throw exception;
		}
	}

	private void validar(UsuarioCadastroRequest request) {
		if (request == null) {
			throw new DadosInvalidosException("Os dados do cadastro não foram enviados.");
		}
		if (estaVazio(request.nome())) {
			throw new DadosInvalidosException("O nome é obrigatório.");
		}
		if (request.nome().trim().length() > TAMANHO_MAXIMO_NOME) {
			throw new DadosInvalidosException("O nome deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres.");
		}
		if (estaVazio(request.email())) {
			throw new DadosInvalidosException("O e-mail é obrigatório.");
		}
		String email = request.email().trim();
		if (email.length() > TAMANHO_MAXIMO_EMAIL) {
			throw new DadosInvalidosException("O e-mail deve ter no máximo " + TAMANHO_MAXIMO_EMAIL + " caracteres.");
		}
		if (!FORMATO_EMAIL.matcher(email).matches()) {
			throw new DadosInvalidosException("O e-mail informado não é válido.");
		}
		if (estaVazio(request.senha())) {
			throw new DadosInvalidosException("A senha é obrigatória.");
		}
		if (request.senha().length() < TAMANHO_MINIMO_SENHA) {
			throw new DadosInvalidosException("A senha deve ter pelo menos " + TAMANHO_MINIMO_SENHA + " caracteres.");
		}
		if (request.dataNascimento() == null) {
			throw new DadosInvalidosException("A data de nascimento é obrigatória.");
		}
		if (request.dataNascimento().isAfter(LocalDate.now())) {
			throw new DadosInvalidosException("A data de nascimento não pode estar no futuro.");
		}
		String fotoPerfil = textoOpcional(request.fotoPerfil());
		if (fotoPerfil != null && fotoPerfil.length() > TAMANHO_MAXIMO_FOTO_PERFIL) {
			throw new DadosInvalidosException(
					"O endereço da foto de perfil deve ter no máximo " + TAMANHO_MAXIMO_FOTO_PERFIL + " caracteres.");
		}
	}

	private static boolean estaVazio(String valor) {
		return valor == null || valor.isBlank();
	}

	private static String textoOpcional(String valor) {
		return estaVazio(valor) ? null : valor.trim();
	}

	private static boolean violouRestricao(Throwable exception, String nomeRestricao) {
		for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacao
					&& nomeRestricao.equalsIgnoreCase(violacao.getConstraintName())) {
				return true;
			}
		}
		return false;
	}
}
