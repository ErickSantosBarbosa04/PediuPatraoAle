# PediuPatrão

Aplicação de gestão de clientes, produtos e pedidos com Java 21, Spring Boot, Spring Security, Thymeleaf e MongoDB.

## Executar

Requisitos: Java 21 e MongoDB local ou uma instância MongoDB acessível pela aplicação.

O padrão conecta a aplicação a `mongodb://localhost:27017/pizzaria`. Para usar outro servidor, configure `MONGODB_URI` e, se necessário, `MONGODB_DATABASE` no ambiente.

Na primeira execução de um banco vazio, configure a senha inicial do administrador. O usuário é `admin` por padrão; `ADMIN_USERNAME` pode alterá-lo.

PowerShell:

```powershell
$env:ADMIN_USERNAME = "admin"
$env:ADMIN_INITIAL_PASSWORD = "defina-uma-senha-segura"
.\mvnw.cmd spring-boot:run
```

O usuário administrador inicial só é criado quando `ADMIN_INITIAL_PASSWORD` está definido. Em bancos existentes, os perfis antigos `USER` são migrados para `ATENDENTE`; o usuário legado `admin` sem perfil é migrado para `ADMIN`.

## Perfis

| Perfil | Permissões |
| --- | --- |
| `ADMIN` | Gerenciar usuários e perfis, consultar pedidos e acessar auditoria. |
| `GERENTE` | Gerenciar o fluxo de pedidos, conceder descontos, cancelar pedidos, consultar clientes e acessar auditoria. |
| `ATENDENTE` | Cadastrar e atualizar clientes, registrar pedidos e acompanhar o andamento. |

Desconto e cancelamento exigem o perfil `GERENTE` tanto nas páginas quanto nas APIs. O desconto é percentual e limitado a 20%, antes da saída do pedido. O cancelamento exige justificativa e só é permitido antes da saída ou retirada.

## Pedidos e auditoria

Pedidos são recebidos com responsável e horário de entrada. O fluxo válido é `RECEBIDO → EM_PREPARACAO → PRONTO → SAIU_PARA_ENTREGA` ou `RETIRADO → FINALIZADO`. O servidor lê preços dos produtos ativos e calcula subtotal, desconto e total; os valores enviados pelo navegador não definem o preço do pedido.

Os eventos de inclusão e mudança de pedidos, transições, descontos, cancelamentos e alterações de clientes são gravados na coleção MongoDB `auditoria`. Cada evento contém usuário, perfil, horário, entidade, identificador, campos anteriores e novos e justificativa quando aplicável. A consulta é somente leitura e restrita a `ADMIN` e `GERENTE`. Senhas e tokens não são incluídos nesses eventos.

As APIs usam a sessão autenticada do Spring Security. O CSRF permanece ativo para formulários e chamadas de mesma origem.

## Rotas principais da API

| Método e rota | Acesso |
| --- | --- |
| `GET /api/pedidos` | Todos os perfis autenticados |
| `POST /api/pedidos` | `ATENDENTE`, `GERENTE` |
| `PUT /api/pedidos/{id}/status` | `GERENTE` |
| `PATCH /api/pedidos/{id}/desconto` | `GERENTE` |
| `POST /api/pedidos/{id}/cancelamento` | `GERENTE` |
| `GET /api/clientes` | Todos os perfis autenticados |
| `POST /api/clientes`, `PUT /api/clientes/{id}` | `ATENDENTE` |
| `DELETE /api/clientes/{id}` | `ADMIN` |
| `GET /api/auditoria` | `ADMIN`, `GERENTE` |
| `/api/usuarios/**` | `ADMIN` |

As rotas de consulta de produtos exigem autenticação; a gestão de produtos é restrita a `ADMIN` e `GERENTE`.
