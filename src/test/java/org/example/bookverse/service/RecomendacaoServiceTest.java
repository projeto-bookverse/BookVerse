
package org.example.bookverse.service;

import org.example.bookverse.repository.AvaliacaoRepository;
import org.example.bookverse.repository.EstanteUsuarioRepository;
import org.example.bookverse.repository.LivroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private EstanteUsuarioRepository estanteUsuarioRepository;

    @Mock
    private LivroRepository livroRepository;

    @InjectMocks
    private RecomendacaoService recomendacaoService;

    @Test
    void deveRetornarListaVaziaQuandoUsuarioNaoTemAvaliacoesPositivas() {

        UUID usuarioId = UUID.randomUUID();

        when(avaliacaoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId))
                .thenReturn(List.of());

        var resultado = recomendacaoService.buscarLivrosRecomendados(usuarioId);

        assertTrue(resultado.isEmpty());

        verify(estanteUsuarioRepository, never()).findByUsuarioId(usuarioId);
        verify(livroRepository, never()).findByGeneroIn(anySet());
    }
}
