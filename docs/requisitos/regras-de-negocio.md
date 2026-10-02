# Regras de negócio adotadas no backend

O documento de requisitos deixa alguns comportamentos em aberto. Estas são as regras implementadas; mudanças devem ser combinadas com o PO (POAN-124).

## Metas

1. A meta precisa de um responsável: um colaborador **ou** uma equipe (RN05).
2. Ao registrar progresso, a meta é concluída automaticamente quando o valor atual atinge o alvo: `atual ≥ alvo`, ou `atual ≤ alvo` se o indicador da meta for do tipo "menor é melhor".
3. Meta concluída ou cancelada não aceita novo progresso.

## Pontuação, desafios e reconhecimentos

4. Pontos só são lançados a partir de uma regra de pontuação ativa (RN02). A conclusão de desafio credita a recompensa do próprio desafio.
5. Colaborador desligado não recebe pontos, créditos, reconhecimentos nem conquistas e não se inscreve em desafios.
6. Inscrição em desafio só com status "Planejado" ou "Em andamento"; conclusão de participação só com "Em andamento".

## AI Credits

7. Uma atividade é elegível quando tem ao menos uma regra de crédito ativa. Cada registro gera o crédito fixo da regra; a quantidade informada não multiplica o valor.
8. O critério de elegibilidade da regra é descritivo (para o gestor); não é interpretado automaticamente.
9. O saldo é sempre `créditos − débitos`; débito maior que o saldo é recusado.
10. O nível de autonomia é o maior nível cujo mínimo o saldo atual atinge. Se o saldo cair, o nível cai.

## Conquistas e níveis de evolução

11. Critérios automáticos: pontos acumulados, metas individuais concluídas, desafios concluídos, reconhecimentos recebidos, atividades registradas e AI Credits recebidos (débitos não descontam). Critério `MANUAL`: concedida por um gestor.
12. Conquistas automáticas são avaliadas a cada movimentação do colaborador e, ao serem criadas ou reativadas, para a empresa inteira. Cada conquista é obtida uma vez só.
13. Conquistas não são retiradas se o colaborador deixar de cumprir o critério.
14. O nível de evolução é o maior nível cujo mínimo de pontos já foi atingido.

## Análises de gestão

| Alerta | Regra | Severidade |
|---|---|---|
| Meta vencida | Meta em andamento (ou atrasada) com data final já passada | Alta |
| Meta em risco | Metade ou mais do prazo decorrido e percentual atingido 25 pontos abaixo do percentual do prazo (só metas "maior é melhor") | Média |
| Indicador em queda | Piora de 10% ou mais entre os dois últimos valores de uma série (colaborador ou equipe); para "menor é melhor", piora é aumento | Média; Alta a partir de 25% |
| Equipe abaixo da média | Pontos por colaborador ativo da equipe abaixo de metade da média da empresa (com ao menos duas equipes com pessoas) | Média |
| Sem atividade recente | Colaborador ativo, admitido há mais de 30 dias, sem pontos nem atividades registradas nos últimos 30 dias | Baixa |

As regras acima são aplicadas pelo `alert-service` (Node.js) a pedido do backend; se o serviço não responder, o backend aplica as mesmas regras localmente ([ADR-0009](../adr/0009-alert-service-node-para-alertas-de-gestao.md)).

## Permissões

15. O perfil Colaborador só lê desempenho, gamificação e AI Credits (inclui o ranking). Quem lança pontos, créditos, metas e reconhecimentos é Gestor ou Administrador.
16. Auditoria, perfis, empresas e equipes são exclusivos do Administrador.
