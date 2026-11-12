package org.example.bookverse.service;

import org.example.bookverse.dto.EstanteAtualizacaoRequest;
import org.example.bookverse.dto.EstanteRequest;
import org.example.bookverse.dto.EstanteResponse;
import org.example.bookverse.entity.EstanteUsuario;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.entity.Usuario;
import org.example.bookverse.exception.AcessoNegadoException;
import org.example.bookverse.exception.ConflitoException;
import org.example.bookverse.exception.DadosInvalidosException;
import org.example.bookverse.exception.RecursoNaoEncontradoException;
import org.example.bookverse.repository.EstanteUsuarioRepository;
import org.example.bookverse.repository.LivroRepository;
import org.example.bookverse.repository.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class EstanteService {

	private static final String MENSAGEM_DUPLICADO = "Este livro já está na estante do usuário.";
	private static final String RESTRICAO_UNICA = "uq_usuario_livro";

	private final EstanteUsuarioRepository estanteRepository;
	private final UsuarioRepository usuarioRepository;
	private final LivroRepository livroRepository;

	public EstanteService(
			EstanteUsuarioRepository estanteRepository,
			UsuarioRepository usuarioRepository,
			LivroRepository livroRepository) {
		this.estanteRepository = estanteRepository;
		this.usuarioRepository = usuarioRepository;
		this.livroRepository = livroRepository;
	}

	@Transactional
	public EstanteResponse adicionar(EstanteRequest request) {
		validarDatas(request.dataInicio(), request.dataFim());
		Usuario usuario = usuarioRepository.findById(request.usuarioId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
		Livro livro = livroRepository.findById(request.livroId())
				.orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado."));
		if (estanteRepository.existsByUsuarioIdAndLivroId(usuario.getId(), livro.getId())) {
			throw new ConflitoException(MENSAGEM_DUPLICADO);
		}

		EstanteUsuario item = new EstanteUsuario(usuario, livro, request.status());
		item.setDataInicio(request.dataInicio());
		item.setDataFim(request.dataFim());
		try {
			return EstanteResponse.from(estanteRepository.saveAndFlush(item));
		} catch (DataIntegrityViolationException exception) {
			if (violouRestricaoUnica(exception)) {
				throw new ConflitoException(MENSAGEM_DUPLICADO);
			}
			throw exception;
		}
	}

	@Transactional
	public EstanteResponse atualizar(UUID id, EstanteAtualizacaoRequest request) {
		validarDatas(request.dataInicio(), request.dataFim());
		EstanteUsuario item = buscarItem(id);
		validarTitularidade(item, request.usuarioId());
		item.setStatus(request.status());
		item.setDataInicio(request.dataInicio());
		item.setDataFim(request.dataFim());
		return EstanteResponse.from(estanteRepository.saveAndFlush(item));
	}

	@Transactional
	public void remover(UUID id, UUID usuarioId) {
		EstanteUsuario item = buscarItem(id);
		validarTitularidade(item, usuarioId);
		estanteRepository.delete(item);
	}

	@Transactional(readOnly = true)
	public List<EstanteResponse> listarPorUsuario(UUID usuarioId) {
		if (!usuarioRepository.existsById(usuarioId)) {
			throw new RecursoNaoEncontradoException("Usuário não encontrado.");
		}
		return estanteRepository.findByUsuarioId(usuarioId).stream()
				.map(EstanteResponse::from)
				.toList();
	}

	private EstanteUsuario buscarItem(UUID id) {
		return estanteRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Item da estante não encontrado."));
	}

	private static void validarTitularidade(EstanteUsuario item, UUID usuarioId) {
		if (!item.getUsuario().getId().equals(usuarioId)) {
			throw new AcessoNegadoException("Somente o dono do item pode alterá-lo ou removê-lo.");
		}
	}

	private static void validarDatas(LocalDate dataInicio, LocalDate dataFim) {
		if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
			throw new DadosInvalidosException("A data de fim não pode ser anterior à data de início.");
		}
	}

	private static boolean violouRestricaoUnica(Throwable exception) {
		for (Throwable causa = exception; causa != null; causa = causa.getCause()) {
			if (causa instanceof ConstraintViolationException violacao
					&& RESTRICAO_UNICA.equalsIgnoreCase(violacao.getConstraintName())) {
				return true;
			}
		}
		return false;
	}
}
