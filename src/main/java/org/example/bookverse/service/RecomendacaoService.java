package org.example.bookverse.service;

import java.util.Map;
import org.example.bookverse.entity.Livro;
import org.example.bookverse.repository.AvaliacaoRepository;
import org.example.bookverse.repository.EstanteUsuarioRepository;
import org.example.bookverse.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RecomendacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final EstanteUsuarioRepository estanteUsuarioRepository;
    private final LivroRepository livroRepository;

    public RecomendacaoService(
            AvaliacaoRepository avaliacaoRepository,
            EstanteUsuarioRepository estanteUsuarioRepository,
            LivroRepository livroRepository) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.estanteUsuarioRepository = estanteUsuarioRepository;
        this.livroRepository = livroRepository;
    }

    public Set<String> buscarGenerosPreferidos(UUID usuarioId) {
        return avaliacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .filter(avaliacao -> avaliacao.getNota() >= 4)
                .map(avaliacao -> avaliacao.getLivro().getGenero())
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public List<Livro> buscarLivrosRecomendados(UUID usuarioId) {

        Set<String> generos = buscarGenerosPreferidos(usuarioId);

        if (generos.isEmpty()) {
            return List.of();
        }

        Set<UUID> livrosNaEstante = estanteUsuarioRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(estante -> estante.getLivro().getId())
                .collect(Collectors.toSet());

        List<Livro> livrosCandidatos = livroRepository.findByGeneroIn(generos)
                .stream()
                .filter(livro -> !livrosNaEstante.contains(livro.getId()))
                .toList();

        if (livrosCandidatos.isEmpty()) {
            return List.of();
        }

        List<UUID> idsLivros = livrosCandidatos.stream()
                .map(Livro::getId)
                .toList();

        Map<UUID, Double> mediasAvaliacoes = avaliacaoRepository.findByLivroIdIn(idsLivros)
                .stream()
                .collect(Collectors.groupingBy(
                        avaliacao -> avaliacao.getLivro().getId(),
                        Collectors.averagingDouble(avaliacao -> avaliacao.getNota())
                ));

        return livrosCandidatos.stream()
                .filter(livro -> mediasAvaliacoes.getOrDefault(livro.getId(), 0.0) >= 4.0)
                .toList();
    }

    }
