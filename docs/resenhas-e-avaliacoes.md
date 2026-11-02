# Resenhas e avaliações de livros

Cada avaliação pertence a um usuário e a um livro existentes. Um usuário pode
avaliar cada livro uma única vez. A nota aceita valores de 1 a 5, a resenha é
obrigatória e limitada a 5000 caracteres, e `flagSpoiler` informa se o texto
contém spoilers.

## Endpoints

- `POST /avaliacoes`: cadastra avaliação com `usuarioId`, `livroId`, `nota`,
  `textoResenha` e `flagSpoiler`.
- `GET /livros/{id}/avaliacoes`: lista as avaliações de um livro.
- `GET /usuarios/{id}/avaliacoes`: lista as avaliações de um usuário.
- `GET /avaliacoes/{id}`: consulta uma avaliação.
- `PUT /avaliacoes/{id}`: atualiza `nota`, `textoResenha` e `flagSpoiler`;
  recebe `usuarioId` para verificar a titularidade.
- `DELETE /avaliacoes/{id}?usuarioId={id}`: exclui uma avaliação após verificar
  se o ID informado corresponde ao autor.

As respostas de erro usam Problem Details. Avaliações incluem as datas de
criação e atualização.

## Banco de dados

Antes de iniciar a aplicação em um banco existente, aplique a migração
`src/main/resources/db/migration/V1__adiciona_atualizado_em_avaliacoes.sql`,
que adiciona `atualizado_em` à tabela `avaliacoes`. A aplicação não executa
migrações automaticamente.

## Autenticação

O projeto ainda não possui autenticação. A titularidade é comparada com o
`usuarioId` recebido na requisição, mas esse valor pode ser falsificado. A
identificação segura do autor depende de uma futura integração com autenticação
e autorização usando o principal autenticado.
