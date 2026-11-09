package org.example.bookverse.controller;

import org.example.bookverse.dto.RecomendacaoResponse;
import org.example.bookverse.service.RecomendacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/recomendacoes")
public class RecomendacaoController {

    private final RecomendacaoService recomendacaoService;

    public RecomendacaoController(RecomendacaoService recomendacaoService) {
        this.recomendacaoService = recomendacaoService;
    }

    @GetMapping
    public ResponseEntity<List<RecomendacaoResponse>> buscarRecomendacoes(
            @RequestParam UUID usuarioId) {

        List<RecomendacaoResponse> recomendacoes =
                recomendacaoService.buscarLivrosRecomendados(usuarioId)
                        .stream()
                        .map(RecomendacaoResponse::from)
                        .toList();

        return ResponseEntity.ok(recomendacoes);
    }
}