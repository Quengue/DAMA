# ADR-0006: Autenticação simplificada por cabeçalho `X-Colaborador-Id`

- **Status:** Aceita, provisória
- **Data:** 2026-09-28

## Contexto

Os requisitos exigem que cada perfil (Administrador, Gestor, Colaborador) acesse só o que lhe é permitido, mas o mecanismo de login da empresa-cliente (SSO, OAuth2) ainda não foi definido.

## Decisão

Um filtro do Spring Security lê o cabeçalho `X-Colaborador-Id`, carrega as permissões dos perfis do colaborador e as expõe como authorities. Todos os endpoints usam `@PreAuthorize` com permissões (`DESEMPENHO_LER`, `AI_CREDITS_GERENCIAR`...). `GET /api/me` devolve o colaborador e suas permissões para o front montar o menu.

## Alternativas consideradas

- JWT próprio com usuário e senha — o documento não prevê cadastro de senha e a empresa-cliente provavelmente usará SSO.
- Sem autenticação — impediria testar as regras de perfil.

## Consequências

- As regras de autorização já estão no lugar e testadas; trocar o mecanismo de autenticação só mexe no filtro.
- O cabeçalho não prova identidade e as permissões valem para qualquer empresa: não serve para produção com mais de uma empresa-cliente.
