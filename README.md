# BookVerse API

API REST da plataforma BookVerse, em que leitores organizam a estante
pessoal (quero ler, lendo, lido, abandonado) e avaliam livros com nota de 1 a 5
estrelas e resenha.

## Tecnologias

Java 17 · Spring Boot 4.1.1 (Web MVC, Data JPA) · Hibernate 7 · PostgreSQL · Maven Wrapper

## Pré-requisitos

- JDK 17 ou superior
- PostgreSQL em `localhost:5432` com o banco `bookverse` e as tabelas
  `usuarios`, `livros`, `estante_usuario` e `avaliacoes`
- Variável de ambiente `DB_PASSWORD` com a senha do usuário `postgres`

## Como executar

```powershell
$env:DB_PASSWORD = "sua_senha"
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080`.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes rodam no PostgreSQL local e desfazem tudo ao final.

## Estrutura do código

| Pacote | Conteúdo |
| --- | --- |
| `controller` | Endpoints REST |
| `service` | Regras de negócio |
| `dto` | Formatos de entrada e saída da API |
| `repository` | Acesso ao banco |
| `entity` | Entidades que espelham as tabelas |
| `exception` | Exceções e tratamento padronizado de erros |

## Resenhas e avaliações

Cada usuário pode avaliar um livro uma única vez, com uma nota de 1 a 5
estrelas, escrever uma resenha de até 5000 caracteres e indicar se o texto
contém spoilers. A avaliação fica vinculada a um usuário e a um livro
existentes e registra as datas de criação e atualização.

Endpoints disponíveis:

- `POST /avaliacoes`: cadastra uma avaliação.
- `GET /livros/{id}/avaliacoes`: lista as avaliações de um livro.
- `GET /usuarios/{id}/avaliacoes`: lista as avaliações de um usuário.
- `GET /avaliacoes/{id}`: consulta uma avaliação.
- `PUT /avaliacoes/{id}`: atualiza uma avaliação.
- `DELETE /avaliacoes/{id}?usuarioId={id}`: exclui uma avaliação.

Os erros seguem o formato Problem Details. A migração
`src/main/resources/db/migration/V1__adiciona_atualizado_em_avaliacoes.sql`
adiciona a coluna `atualizado_em` à tabela `avaliacoes` e deve ser aplicada ao
banco existente antes de iniciar a aplicação.

O projeto ainda não possui autenticação. A titularidade é conferida pelo ID
informado na requisição, portanto a identificação segura do autor dependerá de
uma futura integração com autenticação.

## Documentação

- [Comandos do projeto](docs/comandos.md), incluindo a carga do catálogo de livros
- [Funcionamento do cadastro de usuários](docs/cadastro-de-usuarios.md)
- [Detalhes das resenhas e avaliações](docs/resenhas-e-avaliacoes.md)
- [Requisições de exemplo](requisicoes.http)
