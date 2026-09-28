# C4 — Nível 3: Componentes do backend

Cada caixa é um módulo em `backend/src/main/java/br/com/dama/intelligence/<módulo>`. Setas contínuas são chamadas à `Facade` de outro módulo; a seta tracejada é o evento de domínio `DesempenhoAtualizadoEvent`.

```mermaid
flowchart TB
    subgraph web["Entrada HTTP"]
        seg["shared.security<br/><small>filtro X-Colaborador-Id,<br/>@PreAuthorize</small>"]
        sessao["sessao<br/><small>GET /api/me</small>"]
    end

    subgraph cadastro["Cadastro"]
        org["organizacao<br/><small>empresas, equipes</small>"]
        colab["colaborador"]
        perfil["perfil<br/><small>perfis e permissões</small>"]
    end

    subgraph desempenho["Desempenho"]
        ind["indicador"]
        meta["meta"]
    end

    subgraph gamificacao["Gamificação"]
        pont["pontuacao"]
        desafio["desafio"]
        rec["reconhecimento"]
        conq["conquista<br/><small>conquistas e<br/>níveis de evolução</small>"]
    end

    ai["aicredit<br/><small>atividades, regras,<br/>saldo, autonomia</small>"]

    subgraph leitura["Leitura consolidada"]
        dash["dashboard<br/><small>colaborador e equipe</small>"]
        ana["analytics<br/><small>rankings, visão consolidada,<br/>análises de gestão</small>"]
    end

    aud["auditoria"]

    seg --> perfil
    sessao --> colab & perfil
    colab --> aud
    ind --> colab & org & aud
    meta --> colab & org & ind & aud
    pont --> colab & org & ai & aud
    desafio --> colab & org & pont & aud
    rec --> colab & org & aud
    ai --> colab & org & aud
    conq --> colab & org & aud
    dash --> colab & org & conq

    meta -. evento .-> conq
    pont -. evento .-> conq
    desafio -. evento .-> conq
    rec -. evento .-> conq
    ai -. evento .-> conq

    classDef modulo fill:#1a1a1a,stroke:#ff6a00,color:#f2f2f2
    class seg,sessao,org,colab,perfil,ind,meta,pont,desafio,rec,conq,ai,dash,ana,aud modulo
```

## Regras de dependência

- Um módulo só importa o pacote `api` de outro (Facade, Views, Commands). Classes de `internal` são package-private.
- `analytics` não chama outros módulos: lê das views de `database/02_views.sql`.
- `conquista` não é chamado por quem gera pontos, metas etc.; ele escuta o evento `DesempenhoAtualizadoEvent` ([ADR-0007](../adr/0007-conquistas-por-eventos-de-dominio.md)).
- `auditoria` é chamado por todos os módulos que alteram dados (RF38).

## Estrutura de um módulo

```
meta/
├── api/
│   ├── MetaController.java      # endpoints REST + @PreAuthorize
│   ├── MetaFacade.java          # contrato público do módulo
│   ├── MetaView.java            # records de saída
│   └── CreateMetaCommand.java   # records de entrada (Bean Validation)
└── internal/
    ├── MetaService.java         # regras de negócio (implementa a Facade)
    └── MetaRepository.java      # SQL via JdbcTemplate
```
