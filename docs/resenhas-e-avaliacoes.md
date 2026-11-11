# Resenhas e avaliações de livros

Cada avaliação pertence a um usuário e a um livro existentes. Um usuário pode
avaliar cada livro uma única vez. A nota aceita valores de 1 a 5, a resenha é
opcional e limitada a 5000 caracteres, e `flagSpoiler` informa se o texto
contém spoilers.

## Endpoints

- `POST /avaliacoes`: cadastra avaliação com `livroId`, `nota`, `textoResenha`
  e `flagSpoiler`. O autor é o usuário do token.
- `GET /livros/{id}/avaliacoes`: lista as avaliações de um livro.
- `GET /usuarios/{id}/avaliacoes`: lista as avaliações de um usuário.
- `GET /avaliacoes/{id}`: consulta uma avaliação.
- `PUT /avaliacoes/{id}`: atualiza `nota`, `textoResenha` e `flagSpoiler`.
- `DELETE /avaliacoes/{id}`: exclui uma avaliação.

Só o autor pode editar ou excluir a própria avaliação.

As respostas de erro usam Problem Details, tratadas pelo `TratadorGlobalDeErros`
(400 dados inválidos, 403 sem permissão, 404 não encontrado, 409 avaliação
repetida). Cada avaliação traz o nome do autor e a data de criação.

## Autenticação

Todas as rotas de avaliação exigem login. O autor de cada operação é sempre o
usuário identificado pelo token de acesso (cabeçalho `Authorization: Bearer
{token}`), nunca um ID enviado na requisição. Ver [Login, permissões e
perfil](login-e-perfil.md).
