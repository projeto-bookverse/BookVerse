Funcionamento do Cadastro de Usuários — BookVerse

O cadastro de usuários do BookVerse é realizado por meio do endpoint POST /usuarios, implementado na classe UsuarioController.java.

1. Recebimento dos dados

A API recebe os dados enviados pelo usuário. Os campos obrigatórios são nome, email, senha e dataNascimento. Os campos fotoPerfil e bio são opcionais e podem ser enviados vazios ou como null, quando aplicável.

O DTO UsuarioCadastroRequest.java define a estrutura dos dados recebidos pela API.

2. Validação das informações

A lógica de validação é implementada em UsuarioService.java. Nessa etapa, o sistema verifica o preenchimento dos campos obrigatórios, remove espaços em branco das extremidades do nome e do e-mail, converte o e-mail para letras minúsculas e verifica se o endereço de e-mail já está cadastrado.

Também são aplicadas as seguintes regras: nome e e-mail com no máximo 150 caracteres, e-mail em formato válido, senha com pelo menos 8 caracteres, data de nascimento que não esteja no futuro e endereço da foto de perfil com no máximo 255 caracteres.

3. Proteção da senha

Por segurança, a senha não é armazenada em texto puro no banco de dados. Antes de ser persistida, ela é transformada em hash utilizando PBKDF2 com SHA-256, conforme a implementação de SenhaHashService.java.

4. Persistência no banco de dados

Após a validação, os dados são armazenados na tabela usuarios do banco de dados do BookVerse. Conforme a implementação de Usuario.java, o identificador do usuário é gerado no formato UUID, a data de cadastro é preenchida automaticamente e a conta recebe o tipo LEITOR.

5. Respostas da API

A API retorna os seguintes códigos HTTP conforme o resultado da operação:

201 Created: cadastro realizado com sucesso.

400 Bad Request: campos obrigatórios ausentes ou dados inválidos, conforme as validações implementadas.

409 Conflict: e-mail já cadastrado.

Os erros seguem o formato Problem Details do Spring: o corpo da resposta traz o código de status, o nome do status e, no campo detail, a mensagem que deve ser exibida ao usuário. O tratamento é centralizado em TratadorGlobalDeErros.java.

A classe UsuarioResponse.java define os dados retornados pela API, sem incluir a senha em texto puro nem seu hash.

6. Verificação do cadastro no banco de dados

Após realizar o cadastro, é possível acessar o banco de dados do BookVerse para verificar se o usuário foi registrado corretamente. Essa consulta permite conferir as informações cadastrais, como identificador, nome, e-mail, data de cadastro e tipo de conta, sem expor dados de autenticação.

Conclusão

O processo de cadastro do BookVerse integra uma API REST, validação de dados, proteção de senhas e persistência em banco de dados relacional. Essa estrutura permite registrar novos usuários de forma organizada, tratar erros de cadastro e evitar a exposição de informações sensíveis nas respostas da API.
