# Evidências de validação — Dashboard Pediu Patrão

## Cenários automatizados preparados

Os cenários abaixo estão implementados em `src/test/java/com/umc/pediupatrao/service/DashboardServiceTest.java` e podem ser executados com `.\mvnw.cmd test` usando JDK 21:

| Cenário | Resultado esperado |
| --- | --- |
| Pedido cancelado junto com pedido válido | Cancelado aparece no total/status; não soma no faturamento, ticket médio nem produto vendido. |
| Pedido com desconto | Receita do ranking de produtos é rateada para coincidir com o valor líquido do pedido. |
| Filtro por status | API de serviço retorna somente pedidos no status selecionado. |
| Data fora do padrão ISO | Erro `400 Bad Request`. |
| Início posterior ao fim | Erro `400 Bad Request`. |
| Status desconhecido | Erro `400 Bad Request`. |
| Intervalo acima de 366 dias | Erro `400 Bad Request`. |

## Verificações realizadas nesta preparação

| Verificação | Resultado |
| --- | --- |
| Inspeção do projeto de origem e das entidades `Pedido`, `ItemPedido` e `StatusPedido` | Concluída; integração usa os campos e status existentes. |
| Conferência das regras de acesso existentes | Concluída; a página e a API foram limitadas a `ADMIN` e `GERENTE`. |
| Sintaxe do JavaScript do dashboard (`node --check`) | Aprovada. |
| Estrutura XML do `pom.xml` e presença dos arquivos centrais | Aprovadas. |
| Smoke test da interface com API simulada | Aprovado: valida consulta com período, atualização dos indicadores, gráficos, ranking, pedidos e agendamento de atualização. |
| Teste automatizado Java/JUnit | Não executado neste ambiente: há apenas Java 8 (`java version 1.8.0_381`), enquanto o projeto exige JDK 21; o Maven Wrapper também não conseguiu criar o cache padrão em `C:\Users\user\.m2\wrapper\dists`. |
| Execução ponta a ponta com MongoDB | Não executada; depende de JDK 21 e de uma instância MongoDB acessível. |

O resultado de JUnit não é declarado como aprovado até a execução em uma máquina com os pré-requisitos acima. A validação reproduzível é `.\mvnw.cmd test`; os relatórios detalhados ficam em `target/surefire-reports/` após a execução.

O smoke test de interface é reproduzível com `node .\scripts\dashboard-ui-smoke-test.cjs .\src\main\resources\static\js\admin-dashboard.js` a partir da pasta do projeto. Ele usa uma resposta JSON de exemplo simulada e não requer conexão com MongoDB.
