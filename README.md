# BookVerse API

API REST da plataforma BookVerse, em que leitores organizam a estante
pessoal (quero ler, lendo, lido, abandonado) e avaliam livros com nota de 1 a 5
estrelas e resenha opcional.

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

## Documentação

- [Comandos do projeto](docs/comandos.md), incluindo a carga do catálogo de livros
- [Funcionamento do cadastro de usuários](docs/cadastro-de-usuarios.md)
- [Resenhas e avaliações](docs/resenhas-e-avaliacoes.md)
- [Requisições de exemplo](requisicoes.http)
