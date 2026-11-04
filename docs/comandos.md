# Comandos do BookVerse

Este documento reúne os comandos padrão para trabalhar com o BookVerse em
Windows/PowerShell.

## Pré-requisitos

- PostgreSQL disponível em `localhost:5432`.
- Banco de dados `bookverse` criado e configurado conforme
  `src/main/resources/application.properties`.
- Java instalado e compatível com a versão definida no `pom.xml` (Java 17).

Os comandos abaixo usam o Maven Wrapper do projeto, então não é necessário
instalar o Maven globalmente.

## Conferir o Maven Wrapper

```powershell
.\mvnw.cmd -version
```

## Executar a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

A aplicação fica disponível em `http://localhost:8080`. A execução precisa
conseguir conectar ao PostgreSQL configurado.

## Executar os testes

## Carregar o catálogo de livros

O catálogo inicial (54 livros, com ISBN, capa e sinopse) fica em
`src/test/resources/seed/livros.json` e é gravado no banco por um teste
desativado por padrão. Para executá-lo:

```powershell
$env:DB_PASSWORD = "sua_senha"
.\mvnw.cmd test "-Dtest=LivroSeedTests" "-Dbookverse.seed=true"
```

- Pode ser executado mais de uma vez: livros já existentes (mesmo ISBN, ou
  mesmo título e autor quando não há ISBN) são atualizados, não duplicados.
- Para conferir no pgAdmin: `SELECT count(*) FROM livros;`

```powershell
.\mvnw.cmd test
```

Para limpar os artefatos anteriores e executar os testes novamente:

```powershell
.\mvnw.cmd clean test
```

## Gerar o pacote

```powershell
.\mvnw.cmd clean package
```

O JAR executável é gerado em `target\BookVerse-0.0.1-SNAPSHOT.jar`. Para
executá-lo:

```powershell
java -jar .\target\BookVerse-0.0.1-SNAPSHOT.jar
```

## Enviar a requisição de cadastro de usuário

O arquivo [`requisicoes.http`](../requisicoes.http) contém uma requisição de
exemplo para cadastrar um usuário. Com a aplicação em execução, abra esse
arquivo no VS Code e use **Send Request** (requer uma extensão compatível com
arquivos `.http`).

O endpoint de exemplo é:

```http
POST http://localhost:8080/usuarios
Content-Type: application/json
```

## Limpar os artefatos de build

```powershell
.\mvnw.cmd clean
```
