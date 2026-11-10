# Login, permissões e perfil

## Como funciona o login

1. O usuário envia e-mail e senha para `POST /autenticacao/login`.
2. A API confere a senha com o hash gravado no cadastro (PBKDF2-SHA256).
3. Se estiver correta, devolve um **token JWT** válido por 2 horas:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "tipo": "Bearer", "expiraEm": "2026-11-03T22:30:00-03:00" }
```

4. Nas próximas requisições, o front-end envia o token no cabeçalho
   `Authorization: Bearer {token}`. Depois que ele expira, é preciso entrar de novo.

O token guarda o ID do usuário e o tipo de conta (`LEITOR` ou `ADMIN`) e é assinado
com a chave da variável de ambiente `JWT_SECRET` (mínimo de 32 caracteres). Sem a
chave, ninguém consegue criar um token válido nem alterar um existente.

E-mail inexistente e senha errada recebem a mesma resposta (`401 E-mail ou senha
inválidos.`), para não revelar quais e-mails estão cadastrados. A resposta também
demora o mesmo tempo nos dois casos: quando o e-mail não existe, a senha é conferida
contra um hash fictício.

## Permissões

| Rota | Quem pode acessar |
| --- | --- |
| `POST /usuarios` (cadastro) e `POST /autenticacao/login` | Qualquer pessoa, sem token |
| `POST /livros` e `PUT /livros/{id}` | Somente contas `ADMIN` |
| `PUT /usuarios/{id}` | Somente o próprio usuário |
| `PUT` e `DELETE /avaliacoes/{id}` | Somente o autor da avaliação |
| Todas as demais | Qualquer usuário logado |

| Situação | Resposta |
| --- | --- |
| Sem token, token inválido ou expirado | 401 `É necessário entrar no sistema para acessar este recurso.` |
| Logado, mas sem permissão para a rota (ex.: leitor cadastrando livro) | 403 `Você não tem permissão para realizar esta ação.` |
| Tentando alterar dados de outro usuário | 403 com a mensagem específica (ex.: `Você só pode editar o próprio perfil.`) |

### Como criar um administrador

Todo cadastro pela API cria uma conta de leitor. Para ter um administrador, cadastre
o usuário normalmente e altere o tipo de conta no banco:

```sql
UPDATE usuarios SET tipo_conta = 'admin' WHERE email = 'admin@bookverse.com';
```

Depois, faça login de novo: o novo token já sai com o tipo `ADMIN`.

## Perfil

- `GET /usuarios/{id}`: consulta o perfil (nome, foto, biografia, data de cadastro e
  tipo de conta). E-mail e data de nascimento só aparecem quando o usuário consulta o
  próprio perfil; no perfil de outra pessoa eles vêm vazios. Senha e hash nunca são devolvidos.
- `PUT /usuarios/{id}`: edita `nome`, `fotoPerfil` e `bio` do próprio perfil.
  O nome é obrigatório (até 150 caracteres) e a foto aceita até 255 caracteres.
  E-mail, senha e tipo de conta não são alterados por esta rota.
