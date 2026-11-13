# Catálogo de livros e estante

Todas as rotas abaixo exigem login (cabeçalho `Authorization: Bearer {token}`).

## Catálogo

- `GET /livros?page=0&size=20`: lista os livros em ordem alfabética de título.
  `page` começa em 0 e `size` tem padrão de 20 (de 1 a 100).
- `GET /livros/{id}`: detalhes de um livro.

Cada livro traz `mediaNotas` (média das avaliações, com uma casa decimal; 0.0 quando
ainda não há avaliações) e `totalAvaliacoes`.

A listagem devolve uma página neste formato:

```json
{
  "conteudo": [
    {
      "id": "…", "titulo": "Dom Casmurro", "autor": "Machado de Assis",
      "isbn": "9788500507007", "capaUrl": "https://covers.openlibrary.org/…",
      "sinopse": "…", "genero": "Romance", "anoPublicacao": 1900, "numPaginas": 268,
      "mediaNotas": 4.3, "totalAvaliacoes": 3
    }
  ],
  "pagina": 0,
  "tamanho": 20,
  "totalItens": 54,
  "totalPaginas": 3
}
```

## Estante

| Rota | O que faz |
| --- | --- |
| `POST /estantes` | Adiciona um livro à estante de quem está logado |
| `PUT /estantes/{id}` | Altera status e datas de um item da própria estante |
| `DELETE /estantes/{id}` | Remove um item da própria estante |
| `GET /usuarios/{id}/estante` | Lista a estante de um usuário |

- Entrada: `livroId` e `status` (obrigatórios na inclusão), `dataInicio` e `dataFim`
  (opcionais, no formato `2026-11-12`). Na alteração, só `status` e datas.
- Status: `QUERO_LER`, `LENDO`, `LIDO` ou `ABANDONADO`.
- Saída: `id`, `usuarioId`, `livroId`, `tituloLivro`, `status`, `dataInicio` e `dataFim`.

### Regras

- O dono do item é sempre o usuário do token. Não há campo de usuário na requisição:
  ninguém consegue mexer na estante de outra pessoa.
- Cada livro aparece uma única vez na estante de cada usuário (`409` se repetir).
- A data de fim não pode ser anterior à data de início (`400`).
- Só o dono altera ou remove o item (`403` para os demais).
- Livro, usuário ou item inexistente: `404`.
