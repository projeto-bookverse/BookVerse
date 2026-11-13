# BookVerse API

API REST da plataforma BookVerse, em que leitores organizam a estante
pessoal (quero ler, lendo, lido, abandonado) e avaliam livros com nota de 1 a 5
estrelas e resenha opcional.

## Tecnologias

Java 17 · Spring Boot 4.1.1 (Web MVC, Data JPA, Security) · Hibernate 7 · PostgreSQL · JWT · Maven Wrapper

## Pré-requisitos

- JDK 17 ou superior
- PostgreSQL em `localhost:5432` com o banco `bookverse` e as tabelas
  `usuarios`, `livros`, `estante_usuario` e `avaliacoes`
- Variável de ambiente `DB_PASSWORD` com a senha do usuário `postgres`
- Variável de ambiente `JWT_SECRET` com a chave que assina os tokens de login (mínimo de 32 caracteres)

## Como executar

```powershell
$env:DB_PASSWORD = "sua_senha"
$env:JWT_SECRET = "uma-chave-longa-e-secreta-com-32-caracteres-ou-mais"
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080`. Somente o cadastro (`POST /usuarios`) e o login
(`POST /autenticacao/login`) funcionam sem token; as demais rotas exigem o cabeçalho
`Authorization: Bearer {token}`.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes rodam no PostgreSQL local e desfazem tudo ao final.

## Estrutura do código

| Pacote | Conteúdo |
| --- | --- |
| `config` | Segurança: regras de acesso e geração dos tokens JWT |
| `controller` | Endpoints REST |
| `service` | Regras de negócio |
| `dto` | Formatos de entrada e saída da API |
| `repository` | Acesso ao banco |
| `entity` | Entidades que espelham as tabelas |
| `exception` | Exceções e tratamento padronizado de erros |

## Documentação

- [Comandos do projeto](docs/comandos.md), incluindo a carga do catálogo de livros
- [Funcionamento do cadastro de usuários](docs/cadastro-de-usuarios.md)
- [Login, permissões e perfil](docs/login-e-perfil.md)
- [Catálogo de livros e estante](docs/catalogo-e-estante.md)
- [Resenhas e avaliações](docs/resenhas-e-avaliacoes.md)
- [Requisições de exemplo](requisicoes.http)
