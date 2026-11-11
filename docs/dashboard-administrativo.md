# Dashboard administrativo — Pediu Patrão

## 1. Visão geral

O dashboard administrativo é uma tela autenticada da aplicação Pediu Patrão. A página `/admin` carrega a interface e consulta a API `GET /api/dashboard/resumo` para apresentar os dados atuais de pedidos e vendas. O recurso está disponível para os perfis `ADMIN` e `GERENTE`.

Os dados vêm da coleção MongoDB `pedidos`; os nomes exibidos no resumo de pedidos vêm da coleção `clientes`. Não há dados fictícios no dashboard e não foi necessária uma alteração no formato persistido dos pedidos.

## 2. Requisitos funcionais

| Código | Requisito | Implementação |
| --- | --- | --- |
| RF01 | Indicadores gerais de vendas | Faturamento, total de pedidos, ticket médio, pedidos em andamento e cancelamentos. |
| RF02 | Análise de vendas por gráficos | Gráfico de linha de faturamento e volume por dia, semana ou mês; gráfico de rosca por situação. |
| RF03 | Situação dos pedidos | Contagem por status e tabela de pedidos recentes com status, cliente, horário e valor. |
| RF04 | Produtos mais vendidos | Ranking dos cinco produtos com mais unidades em pedidos não cancelados, incluindo receita atribuída. |
| RF05 | Filtros de consulta | Data inicial, data final e status; atualiza os cartões, gráficos, ranking e lista em conjunto. |
| RF06 | Integração com a API | A página busca JSON da API da própria aplicação usando a sessão autenticada do Spring Security. |
| RF07 | Atualização e tratamento de dados | Atualização manual e automática a cada 60 segundos; estados de carregamento, ausência de dados e erro. Valores monetários são formatados em reais. |

## 3. Requisitos não funcionais

| Código | Requisito |
| --- | --- |
| RNF01 | Segurança: página e API exigem autenticação e são restritas a `ADMIN` e `GERENTE`; as rotas continuam sob proteção CSRF e sessão do Spring Security. |
| RNF02 | Integridade: valores são calculados a partir dos pedidos persistidos. Pedidos cancelados são contabilizados na quantidade e no gráfico de volume, mas não no faturamento, ticket médio ou unidades vendidas. |
| RNF03 | Precisão temporal: os filtros usam datas de calendário no fuso `America/Sao_Paulo`; início é inclusivo e o fim inclui o dia completo. |
| RNF04 | Limite operacional: cada consulta aceita no máximo 366 dias consecutivos e busca no MongoDB somente pedidos no intervalo informado. |
| RNF05 | Usabilidade: interface responsiva para telas menores, em português brasileiro, com estados vazios e mensagens de erro legíveis. |
| RNF06 | Compatibilidade: interface servida pela própria aplicação e dependências locais; não requer serviço externo de gráficos ou fonte. |

## 4. Regras de cálculo e atualização

- A data usada para localizar um pedido é `dataHoraEntrada`. Registros sem esse campo não entram nos resultados.
- O faturamento soma `valorTotal`; se esse campo estiver ausente em um pedido legado, usa `subtotal`. Pedidos sem ambos os valores somam zero.
- `totalPedidos` inclui todos os status encontrados no intervalo. `pedidosEmAndamento` conta `RECEBIDO`, `EM_PREPARACAO` e `PRONTO`.
- `ticketMedio` é o faturamento dividido pela quantidade de pedidos não cancelados. O resultado é arredondado para duas casas decimais.
- O gráfico de vendas conta todos os pedidos por período e exclui os cancelados do valor de faturamento.
- Os produtos são lidos dos itens do pedido. Para documentos antigos sem `itens`, o ranking usa `sabor`/`tipoPizza` e `quantidade`. A receita dos itens é ajustada proporcionalmente ao `valorTotal` do pedido para considerar descontos.
- A série agrupa em dia para até 31 dias, semana para 32 a 180 dias e mês para 181 a 366 dias. Sem dados, o eixo continua mostrando o período selecionado e os valores ficam zerados.
- A atualização automática ocorre a cada 60 segundos; o botão de atualização e o botão de filtro consultam o mesmo endpoint.

## 5. API

### `GET /api/dashboard/resumo`

Retorna um único resumo para alimentar cartões, gráficos, ranking e pedidos recentes.

**Acesso:** sessão autenticada com perfil `ADMIN` ou `GERENTE`.

**Parâmetros de consulta:**

| Parâmetro | Obrigatório | Formato | Regra |
| --- | --- | --- | --- |
| `inicio` | Não | `AAAA-MM-DD` | Primeiro dia do intervalo, inclusivo. Padrão: hoje menos seis dias. |
| `fim` | Não | `AAAA-MM-DD` | Último dia do intervalo, inclusivo. Padrão: hoje. |
| `status` | Não | Enum de status | Filtra as métricas e listas para um status. Se omitido ou vazio, inclui todos. |

Datas são interpretadas no fuso `America/Sao_Paulo`. Se apenas uma data for enviada, o padrão da outra continua sendo calculado em relação ao dia atual. O intervalo inicial deve ser anterior ou igual ao final e pode ter no máximo 366 dias. Os status aceitos são `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO` e `CANCELADO`. Para compatibilidade com nomes antigos, `EM_PREPARO` equivale a `EM_PREPARACAO` e `ENTREGUE` equivale a `FINALIZADO`.

**Exemplo de requisição:**

```http
GET /api/dashboard/resumo?inicio=2026-10-30&fim=2026-10-30&status=FINALIZADO
Accept: application/json
Cookie: JSESSIONID=<sessao autenticada>
```

**Exemplo de resposta `200 OK`:**

```json
{
  "periodo": { "inicio": "2026-10-30", "fim": "2026-10-30" },
  "statusSelecionado": "FINALIZADO",
  "granularidade": "DIA",
  "indicadores": {
    "faturamento": 258.50,
    "totalPedidos": 3,
    "ticketMedio": 86.17,
    "pedidosEmAndamento": 0,
    "pedidosCancelados": 0
  },
  "vendas": [
    { "periodo": "2026-10-30", "rotulo": "30/10", "faturamento": 258.50, "pedidos": 3 }
  ],
  "pedidosPorStatus": [
    { "codigo": "RECEBIDO", "nome": "Recebido", "quantidade": 0 },
    { "codigo": "EM_PREPARACAO", "nome": "Em preparo", "quantidade": 0 },
    { "codigo": "PRONTO", "nome": "Pronto", "quantidade": 0 },
    { "codigo": "SAIU_PARA_ENTREGA", "nome": "Saiu para entrega", "quantidade": 0 },
    { "codigo": "RETIRADO", "nome": "Retirado", "quantidade": 0 },
    { "codigo": "FINALIZADO", "nome": "Finalizado", "quantidade": 3 },
    { "codigo": "CANCELADO", "nome": "Cancelado", "quantidade": 0 }
  ],
  "produtosMaisVendidos": [
    { "produtoId": "produto-123", "nome": "Pizza de queijo", "quantidade": 4, "faturamento": 210.00 }
  ],
  "pedidosRecentes": [
    {
      "id": "pedido-456",
      "cliente": "Ana Souza",
      "status": "FINALIZADO",
      "statusNome": "Finalizado",
      "dataHoraEntrada": "2026-10-30T18:45:00Z",
      "valorTotal": 86.50
    }
  ],
  "atualizadoEm": "2026-10-30T19:00:00Z"
}
```

`vendas` contém um ponto para cada dia, semana ou mês do intervalo. No intervalo filtrado por `FINALIZADO`, a distribuição por status mostra os pedidos finalizados que passaram pelo filtro.

**Erros esperados:**

| HTTP | Quando ocorre | Formato do corpo |
| --- | --- | --- |
| `400 Bad Request` | Data malformada, período invertido, mais de 366 dias, data final fora do intervalo suportado ou status desconhecido. | `{"instante":"2026-10-30T19:00:00Z","status":400,"erro":"Bad Request","mensagem":"O parâmetro 'inicio' deve usar o formato AAAA-MM-DD.","caminho":"/api/dashboard/resumo"}` |
| `302 Found` | Usuário sem sessão: o Spring Security encaminha o navegador para `/login`. | Redirecionamento para a tela de login. |
| `403 Forbidden` | Sessão válida sem perfil `ADMIN` ou `GERENTE`. | Resposta de acesso negado do Spring Security. |
| `500 Internal Server Error` | Falha inesperada da aplicação ou da conexão com MongoDB. | Erro padrão do Spring Boot. |

### Rotas de tela relacionadas

| Método e rota | Uso | Acesso |
| --- | --- | --- |
| `GET /admin` | Carrega o dashboard, que consulta a API acima. | `ADMIN`, `GERENTE` |
| `GET /pedidos` | Abre a gestão e consulta operacional de pedidos. | Perfis autenticados conforme a configuração existente. |
| `GET /produtos` | Abre a gestão de produtos a partir do link no painel. | `ADMIN`, `GERENTE` |

## 6. Como executar e validar

Pré-requisitos: JDK 21 e MongoDB local ou acessível. Configure a senha inicial do administrador em `ADMIN_INITIAL_PASSWORD` antes da primeira execução, conforme as instruções do `README.md` principal.

PowerShell, a partir da pasta do projeto:

```powershell
$env:ADMIN_INITIAL_PASSWORD = "defina-uma-senha-segura"
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

O teste de integração da interface com uma resposta simulada pode ser executado sem dependências npm:

```powershell
node .\scripts\dashboard-ui-smoke-test.cjs .\src\main\resources\static\js\admin-dashboard.js
```

Entre com uma conta `ADMIN` ou `GERENTE` e acesse `/admin`. A página inicia com os últimos sete dias, permite alterar datas e status, atualiza manualmente e busca novos dados a cada minuto.

## 7. Evidências e cenários de teste

Os testes automatizados de unidade estão em `src/test/java/com/umc/pediupatrao/service/DashboardServiceTest.java`. Eles cobrem: exclusão de cancelados do faturamento e do ranking; rateio de desconto nos produtos; filtro por status; e rejeição de datas inválidas, períodos invertidos, status desconhecido e intervalos acima de 366 dias.

Para gerar a evidência de execução no ambiente de desenvolvimento, rode `.\mvnw.cmd test`. O Maven apresenta o resultado final do JUnit e salva os relatórios em `target/surefire-reports/`. Consulte também `docs/evidencias-testes-dashboard.md` para o registro de validações e pré-requisitos do ambiente em que esta entrega foi preparada.
